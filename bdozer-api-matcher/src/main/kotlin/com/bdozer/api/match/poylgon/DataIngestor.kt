package com.bdozer.api.match.poylgon

import com.bdozer.api.match.HttpClientExtensions.readEntity
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.HttpClient
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.lang.System.getenv

@Service
class DataIngestor(
    private val restHighLevelClient: RestHighLevelClient,
    private val httpClient: HttpClient,
) : CommandLineRunner {

    private val objectMapper = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

    private val log = LoggerFactory.getLogger(DataIngestor::class.java)
    private val polygonKey = getenv("POLYGON_API_KEY") ?: error("environment POLYGON_API_KEY not defined")

    override fun run(vararg args: String) {
        ClassPathResource("tickers.txt")
            .inputStream
            .bufferedReader()
            .forEachLine { line ->
                nofail {
                    val ticker = line.trim()
                    log.info("Ingesting $ticker")
                    val entity =
                        httpClient.readEntity<TickerDetail>("https://api.polygon.io/v1/meta/symbols/$ticker/company?&apiKey=$polygonKey")
                    val json = objectMapper.writeValueAsString(entity)
                    val index = "ticker-details"

                    restHighLevelClient.index(
                        IndexRequest(index)
                            .id(ticker)
                            .source(json, XContentType.JSON),
                        RequestOptions.DEFAULT
                    )

                    val node = restHighLevelClient.lowLevelClient.nodes.first()
                    log.info("Ingested $ticker at ${node.host}/$index/_doc/$ticker")
                }
            }
    }

    fun nofail(block: () -> Unit) {
        val log = LoggerFactory.getLogger(DataIngestor::class.java)
        try {
            block.invoke()
        } catch (e: Exception) {
            log.error(e.message)
        }
    }
}
