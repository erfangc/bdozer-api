package com.starburst.starburst.edgar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.starburst.starburst.edgar.factbase.ingestor.FilingIngestor
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.concurrent.Executors

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
    private val objectMapper: ObjectMapper,
    private val filingIngestor: FilingIngestor
) {

    data class EdgarFilingMetadata(
        val ciks: List<String> = emptyList(),
        val period_ending: LocalDate,
        val root_form: String? = null,
        val file_num: List<String> = emptyList(),
        val display_names: List<String> = emptyList(),
        val sequence: String? = null,
        val biz_states: List<String> = emptyList(),
        val sics: List<String> = emptyList(),
        val form: String,
        val adsh: String,
        val biz_locations: List<String> = emptyList(),
        val file_date: LocalDate,
        val file_type: String? = null,
        val file_description: String? = null,
        val inc_states: List<String> = emptyList()
    )

    private val executor = Executors.newCachedThreadPool()

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
        val paddedCik = (0 until (10 - cik.length)).joinToString("") { "0" } + cik

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

    @PostMapping("bootstrap-filing-entity")
    fun bootstrapFilingEntity(@RequestParam cik: String): Unit {
        //
        // Find the most recent 4 10-Qs and the most recent 10K and ingest those
        //
        val hits = (searchFilings(cik).at("/hits/hits") as ArrayNode).map {
            objectMapper.treeToValue<EdgarFilingMetadata>(
                it.at("/_source")
            )!!
        }
        val tenKs = hits.filter { it.form == "10-K" }.sortedByDescending { it.period_ending }
        val tenQs = hits.filter { it.form == "10-Q" }.sortedByDescending { it.period_ending }
        val recent10Ks = tenKs.first()
        val recent10Qs = tenQs.subList(0, 4)

        executor.submit {
            filingIngestor.ingestFiling(
                cik = cik,
                adsh = recent10Ks.adsh
            )
            for (recent10Q in recent10Qs) {
                filingIngestor.ingestFiling(
                    cik = cik,
                    adsh = recent10Q.adsh
                )
            }
        }
    }
}