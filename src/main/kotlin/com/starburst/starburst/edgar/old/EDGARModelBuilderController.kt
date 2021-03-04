package com.starburst.starburst.edgar.old

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.starburst.starburst.models.Model
import com.starburst.starburst.edgar.dataclasses.MetaLink
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

@RestController
@CrossOrigin
@RequestMapping("api/edgar-model-builder")
class EDGARModelBuilderController(
    private val http: HttpClient,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(EDGARModelBuilderController::class.java)

    //
    // at launch create the us-gaap XSD
    //
    private val gaapXsdLink = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd"
    private val usGaapXsdBytes = readLink(gaapXsdLink)

    /**
     * A pass through to the EDGAR Elasticsearch search end point
     * this fixes any CORS issues for the UI as well
     */
    @GetMapping("search", produces = ["application/json"])
    fun search(@RequestParam term: String): JsonNode {
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()
        entity.content = "{\"keysTyped\": \"$term\"}".byteInputStream()
        httpPost.entity = entity
        val jsonNode = objectMapper.readTree(http.execute(httpPost).entity.content)
        httpPost.releaseConnection()
        return jsonNode
    }

    @GetMapping("{cik}")
    fun buildModel(@PathVariable cik: String): Model {
        val adsh = adsh(cik)
        val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}"

        val metaLinkGetRequest = HttpGet("${baseUrl}/MetaLinks.json")
        val metaLink = try {
            objectMapper
                .readValue<MetaLink>(http.execute(metaLinkGetRequest).entity.content)
        } catch (e: Exception) {
            error("Unable to find MetaLinks for $adsh")
        }
        metaLinkGetRequest.releaseConnection()

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

        return EDGARXbrlToModelTranslator(
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
                    "10-K"
                  ]
                } 
            """.trimIndent().byteInputStream()

        val jsonNode = try {
            httpPost.entity = entity
            val content = http.execute(httpPost).entity.content
            objectMapper.readTree(content)
        } catch (e: Exception) {
            error("unable to find latest adsh for $cik")
        }
        httpPost.releaseConnection()

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

