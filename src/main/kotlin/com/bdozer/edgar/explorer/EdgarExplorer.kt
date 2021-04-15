package com.bdozer.edgar.explorer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.bdozer.edgar.explorer.dataclasses.EdgarEntity
import com.bdozer.edgar.explorer.dataclasses.EdgarFilingMetadata
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
import org.springframework.stereotype.Service

@Service
class EdgarExplorer(
    private val http: HttpClient,
    private val objectMapper: ObjectMapper
) {

    fun searchEntities(term: String): List<EdgarEntity?> {
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()
        entity.content = "{\"keysTyped\": \"$term\"}".byteInputStream()
        httpPost.entity = entity
        val jsonNode = objectMapper.readTree(http.execute(httpPost).entity.content)
        httpPost.releaseConnection()
        val arrayNodes = jsonNode.at("/hits/hits")
        if (!arrayNodes.isArray) {
            return emptyList()
        }
        return (arrayNodes as ArrayNode)
            .mapNotNull { node ->
                try {
                    objectMapper.treeToValue<EdgarEntity>(node)
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it._source.tickers != null }
            .map { edgarEntity ->
                edgarEntity.copy(
                    _source = edgarEntity._source.copy(
                        tickers = edgarEntity
                            ._source
                            .tickers
                            ?.split(",")
                            ?.first()
                            ?.trim()
                    )
                )
            }
    }

    fun searchFilings(cik: String): List<EdgarFilingMetadata> {
        /*
        Just query the SEC's Elasticsearch servers for the latest filing
         */
        val httpPost = HttpPost("https://efts.sec.gov/LATEST/search-index")
        val entity = BasicHttpEntity()

        /*
        Pad with leading 0 to make 10 digits
         */
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
            error("unable to find latest adsh for $cik, error: ${e.message}")
        }
        httpPost.releaseConnection()

        val arrayNode = jsonNode.at("/hits/hits") as ArrayNode
        return arrayNode.map {
            objectMapper.treeToValue<EdgarFilingMetadata>(
                it.at("/_source")
            )!!
        }
    }

    fun latestFiscalFiling(ciK: String): EdgarFilingMetadata? {
        return searchFilings(ciK)
            .filter { it.form == "10-K" }
            .maxByOrNull { it.period_ending }
    }

}