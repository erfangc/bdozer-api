package com.bdozer.api.stockanalysis.support.poylgon

import com.bdozer.api.match.poylgon.PreviousClose
import com.bdozer.api.match.poylgon.TickerDetail
import com.bdozer.api.stockanalysis.HttpClientExtensions.readEntity
import org.apache.http.client.HttpClient
import org.slf4j.LoggerFactory
import java.lang.System.getenv

class PolygonService(private val httpClient: HttpClient) {

    private val log = LoggerFactory.getLogger(PolygonService::class.java)
    private val polygonKey = getenv("POLYGON_API_KEY")
        ?: error("environment POLYGON_API_KEY not defined")

    fun tickerDetails(ticker: String): TickerDetail {
        log.info("Calling ticker details on ticker={}", ticker)
        return httpClient.readEntity(
            "https://api.polygon.io/v1/meta/symbols/$ticker/company?&apiKey=$polygonKey"
        )
    }

    fun previousClose(ticker: String): PreviousClose {
        log.info("Calling ticker previous close on ticker={}", ticker)
        return httpClient.readEntity(
            "https://api.polygon.io/v2/aggs/ticker/$ticker/prev?adjusted=true&apiKey=$polygonKey"
        )
    }

}
