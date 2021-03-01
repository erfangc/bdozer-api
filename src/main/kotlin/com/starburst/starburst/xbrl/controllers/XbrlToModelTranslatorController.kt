package com.starburst.starburst.xbrl.controllers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.starburst.starburst.models.Model
import com.starburst.starburst.xbrl.MetaLink
import com.starburst.starburst.xbrl.XbrlToModelTranslator
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

@RestController
@CrossOrigin
@RequestMapping("api/xbrl-to-model-translator")
class XbrlToModelTranslatorController(
    private val http: HttpClient,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(XbrlToModelTranslatorController::class.java)

    //
    // at launch create the us-gaap XSD
    //
    private val gaapXsdLink = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd"
    private val usGaapXsdBytes = readLink(gaapXsdLink)

    @GetMapping("search", produces = ["application/json"])
    fun search(@RequestParam term: String): JsonNode {
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()
        entity.content = "{\"keysTyped\": \"$term\"}".byteInputStream()
        httpPost.entity = entity
        return objectMapper.readTree(http.execute(httpPost).entity.content)
    }

    @GetMapping("{cik}")
    fun parseModel(@PathVariable cik: String): Model {
        val adsh = adsh(cik)
        val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}"
        val metaLink = objectMapper
            .readValue<MetaLink>(http.execute(HttpGet("${baseUrl}/MetaLinks.json")).entity.content)

        val instance = metaLink.instance.entries.first()
        val dts = instance.value.dts

        val instanceFileName = dts.inline.local.first().replace(".htm", "_htm.xml")
        val extensionXsdFileName = dts.schema.local.first()
        val calculationFileName = dts.calculationLink.local.first()
        val labelFileName = dts.labelLink.local.first()
        val definitionFileName = dts.definitionLink.local.first()

        //
        // resolve the locations of the XBRL files based on the information above
        //
        val instanceFile = readLink("${baseUrl}/$instanceFileName")
        val extensionXsdFile = readLink("${baseUrl}/$extensionXsdFileName")
        val labelFile = readLink("${baseUrl}/$labelFileName")
        val definitionFile = readLink("${baseUrl}/$definitionFileName")
        val calculationFile = readLink("${baseUrl}/$calculationFileName")

        return XbrlToModelTranslator(
            usGaapXsdStream = ByteArrayInputStream(usGaapXsdBytes),
            instanceStream = ByteArrayInputStream(instanceFile),
            extensionXsdStream = ByteArrayInputStream(extensionXsdFile),
            calculationStream = ByteArrayInputStream(calculationFile),
            labelStream = ByteArrayInputStream(labelFile),
            definitionStream = ByteArrayInputStream(definitionFile),
        ).translate()

    }

    private fun adsh(cik: String): String {

        // just query the SEC's Elasticsearch servers for the latest filing
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()

        // pad with leading 0 to make 10 digits
        val paddedCik = (0 until (10 - cik.length)).joinToString("") {"0"}  + cik

        entity.content = """
                {
                  "ciks": [
                    "$paddedCik"
                  ],
                  "forms": [
                    "10-K",
                    "10-Q"
                  ]
                } 
            """.trimIndent().byteInputStream()

        httpPost.entity = entity
        val content = http.execute(httpPost).entity.content
        val jsonNode = objectMapper.readTree(content)
        return (jsonNode.at("/hits/hits") as ArrayNode)[0].at("/_source/adsh").asText()
    }

    private fun readLink(link: String): ByteArray? {
        log.info("Reading remote link $link")
        val get = HttpGet(link)
        val httpResponse = http.execute(get)
        val entity = httpResponse.entity
        val allBytes = entity.content.readAllBytes()
        get.releaseConnection()
        return allBytes
    }
}

