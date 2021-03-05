package com.starburst.starburst.edgar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.springframework.web.bind.annotation.*

/**
 * This REST Controller basically is a pass through to the SEC's own Elasticsearch
 * APIs to find entities by symbol, name CIK as well as their filings
 *
 *  We do a pass through here to cure any CORS issues
 */
@RestController
@CrossOrigin
@RequestMapping("public/edgar-explorer")
class EdgarExplorerController(
    private val http: HttpClient,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("entities", produces = ["application/json"])
    fun searchEntities(@RequestParam term: String): JsonNode {
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()
        entity.content = "{\"keysTyped\": \"$term\"}".byteInputStream()
        httpPost.entity = entity
        val jsonNode = objectMapper.readTree(http.execute(httpPost).entity.content)
        httpPost.releaseConnection()
        return jsonNode
    }

    @GetMapping("filings", produces = ["application/json"])
    fun searchFilings(@RequestParam cik: String): JsonNode {
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

        val jsonNode = try {
            httpPost.entity = entity
            val content = http.execute(httpPost).entity.content
            objectMapper.readTree(content)
        } catch (e: Exception) {
            error("unable to find latest adsh for $cik")
        }
        httpPost.releaseConnection()

        return jsonNode
    }
}