package com.starburst.starburst.edgar.explorer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.starburst.starburst.edgar.explorer.dataclasses.EdgarEntity
import com.starburst.starburst.edgar.explorer.dataclasses.EdgarFilingMetadata
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
        return (jsonNode.at("/hits/hits") as ArrayNode).map { node ->
            objectMapper.treeToValue<EdgarEntity>(node)
        }.filter { it?._source?.tickers != null }.map {
            it?.copy(
                _source = it._source.copy(tickers = it._source.tickers?.split(",")?.first()?.trim())
            )
        }
    }

    fun searchFilings(cik: String): List<EdgarFilingMetadata> {
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

        return (jsonNode.at("/hits/hits") as ArrayNode).map {
            objectMapper.treeToValue<EdgarFilingMetadata>(
                it.at("/_source")
            )!!
        }
    }
}