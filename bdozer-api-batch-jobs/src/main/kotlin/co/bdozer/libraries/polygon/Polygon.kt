package co.bdozer.libraries.polygon

import co.bdozer.libraries.polygon.models.PreviousClose
import co.bdozer.libraries.polygon.models.TickerDetailV3
import co.bdozer.libraries.utils.Beans
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

object Polygon {

    private val objectMapper = Beans.objectMapper()
    private val httpClient = Beans.httpClient()
    private val apiKey = System.getenv("POLYGON_API_KEY") ?: error("missing environment variable POLYGON_API_KEY")

    fun tickerDetailV3(ticker: String): TickerDetailV3 {
        val httpResponse = httpClient.send(
            HttpRequest
                .newBuilder(URI.create("https://api.polygon.io/v3/reference/tickers/$ticker?apiKey=$apiKey"))
                .GET()
                .build(),
            BodyHandlers.ofInputStream(),
        )
        val inputStream = httpResponse.body()
        return objectMapper.readValue(inputStream)
    }

    fun previousClose(ticker: String): PreviousClose {
        val httpResponse = httpClient.send(
            HttpRequest
                .newBuilder(URI.create("https://api.polygon.io/v2/aggs/ticker/$ticker/prev?adjusted=true&apiKey=$apiKey"))
                .GET()
                .build(),
            BodyHandlers.ofInputStream(),
        )
        val inputStream = httpResponse.body()
        return objectMapper.readValue(inputStream)
    }

}

