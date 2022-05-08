package com.bdozer.api.stockanalysis.support.poylgon

import com.bdozer.api.match.poylgon.PreviousClose
import com.bdozer.api.match.poylgon.TickerDetail
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.lang.System.getenv
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class PolygonService {

    private val objectMapper = jacksonObjectMapper()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val httpClient = HttpClient.newHttpClient()
    private val log = LoggerFactory.getLogger(PolygonService::class.java)
    private val polygonKey = getenv("POLYGON_API_KEY")
        ?: error("environment POLYGON_API_KEY not defined")
    
    fun tickerDetails(ticker: String): TickerDetail {
        log.info("Calling ticker details on ticker={}", ticker)
        val uri = URI.create("https://api.polygon.io/v1/meta/symbols/$ticker/company?&apiKey=$polygonKey")
        val httpResponse = httpClient
            .send(
                HttpRequest
                    .newBuilder(uri)
                    .GET()
                    .build(),
                BodyHandlers.ofInputStream(),
            )
        if (httpResponse.statusCode() > 200) {
            val responseBody = httpResponse.body().bufferedReader().readText()
            error("Error executing GET $uri statusCode=${httpResponse.statusCode()} responseBody='${responseBody}'")
        }
        return objectMapper.readValue(httpResponse.body()) 
    }

    fun previousClose(ticker: String): PreviousClose {
        log.info("Calling ticker previous close on ticker={}", ticker)
        val uri = URI.create("https://api.polygon.io/v2/aggs/ticker/$ticker/prev?adjusted=true&apiKey=$polygonKey")
        val httpResponse = httpClient
            .send(
                HttpRequest
                    .newBuilder(URI.create("https://api.polygon.io/v2/aggs/ticker/$ticker/prev?adjusted=true&apiKey=$polygonKey"))
                    .GET()
                    .build(),
                BodyHandlers.ofInputStream(),
            )
        if (httpResponse.statusCode() > 200) {
            val responseBody = httpResponse.body().bufferedReader().readText()
            error("Error executing GET $uri statusCode=${httpResponse.statusCode()} responseBody='${responseBody}'")
        }
        return objectMapper.readValue(httpResponse.body())
    }

}
