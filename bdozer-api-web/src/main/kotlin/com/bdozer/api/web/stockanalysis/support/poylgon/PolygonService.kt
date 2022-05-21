package com.bdozer.api.web.stockanalysis.support.poylgon

import com.bdozer.api.match.poylgon.PreviousClose
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.System.getenv
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import com.fasterxml.jackson.annotation.JsonProperty

@Service
class PolygonService {

    private val objectMapper = jacksonObjectMapper()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val httpClient = HttpClient.newHttpClient()
    private val log = LoggerFactory.getLogger(PolygonService::class.java)
    private val polygonKey = getenv("POLYGON_API_KEY")
        ?: error("environment POLYGON_API_KEY not defined")
    
    fun tickerDetails(ticker: String): TickerDetailV3 {
        log.info("Calling ticker details on ticker={}", ticker)
        val uri = URI.create("https://api.polygon.io/v3/reference/tickers/$ticker?&apiKey=$polygonKey")
        val httpResponse = httpClient
            .send(
                HttpRequest
                    .newBuilder(uri)
                    .GET()
                    .build(),
                BodyHandlers.ofString(),
            )
        if (httpResponse.statusCode() > 200) {
            error("Failed to get ticker detail v3 responseBody='${httpResponse.body()}'")
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
                BodyHandlers.ofString(),
            )
        if (httpResponse.statusCode() > 200) {
            error("Failed to get previous close responseBody='${httpResponse.body()}'")
        }
        return objectMapper.readValue(httpResponse.body())
    }

}
