package com.starburst.starburst.xbrl.controllers

import com.mongodb.client.MongoClient
import com.starburst.starburst.xbrl.utils.NodeListExtension.findByTag
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.bson.Document
import org.json.XML
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.w3c.dom.Node
import java.io.StringWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@RequestMapping("api/edgar-rss-processor")
@CrossOrigin
@RestController
class EdgarRssProcessorController(private val http: HttpClient, mongo: MongoClient) {

    private val database = mongo.getDatabase("starburst")
    private val col = database.getCollection("edgar-filings")
    private val factory = DocumentBuilderFactory.newInstance()
    private val builder = factory.newDocumentBuilder()
    private val executor = Executors.newCachedThreadPool()

    @PostMapping("run-for-last-twelve-month")
    fun runForLastTwelveMonth() {
        executor.submit {
            log.info("processing edgar filings for the last 12 months via their RSS feed see them at https://www.sec.gov/Archives/edgar/monthly")
            val now = LocalDate.now()
            for (i in 0..12) {
                val date = now.minusMonths(i.toLong())
                val link = "https://www.sec.gov/Archives/edgar/monthly/xbrlrss-${date.year}-${
                    date.format(
                        DateTimeFormatter.ofPattern("MM")
                    )
                }.xml"
                processLink(link)
            }
        }
    }

    private val log = LoggerFactory.getLogger(EdgarRssProcessorController::class.java)
    private fun processLink(link: String) {
        log.info("processing $link")
        val document = builder
            .parse(http.execute(HttpGet(link)).entity.content)
        val items = document.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i)
            val formType = item.findByTag("edgar:xbrlFiling")?.findByTag("edgar:formType")?.textContent
            if (formType == "10-Q") {
                val json = XML.toJSONObject(convertNodeToString(item)).toString()
                val document1 = Document.parse(json)
                document1["_id"] = item.findByTag("guid")?.textContent ?: ""
                col.save(document = document1)
            }
        }
        log.info("done processing $link")
    }

    /**
     * You've got to be kidding me Java ... there isn't a quick method to
     * just do this?
     */
    private fun convertNodeToString(node: Node): String {
        try {
            val writer = StringWriter()
            val trans: Transformer = TransformerFactory.newInstance().newTransformer()
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            trans.setOutputProperty(OutputKeys.INDENT, "yes")
            trans.transform(DOMSource(node), StreamResult(writer))
            return writer.toString()
        } catch (te: TransformerException) {
            te.printStackTrace()
        }
        return ""
    }

}
