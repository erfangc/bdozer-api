package com.bdozer.alphavantage

import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.extensions.JsonExtensions.readJson
import com.fasterxml.jackson.databind.JsonNode
import org.apache.http.client.HttpClient
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AlphaVantageService(private val httpClient: HttpClient) {

    val apiKey: String =
        System.getenv("ALPHA_VANTAGE_API_KEY") ?: error("environment ALPHA_VANTAGE_API_KEY not defined")

    private val rowCache = ClassPathResource("us_equity_beta.csv")
        .inputStream
        .bufferedReader()
        .readLines()
        .map {
            line ->
            val split = line.split(",")
            val ticker = split[0]
            val price = split[1].toDoubleOrNull().orZero()
            val beta = split[2].toDoubleOrNull() ?: 1.0
            Row(ticker,price, beta)
        }
        .associateBy { it.ticker }

    data class Row(
        val ticker: String,
        val price: Double,
        val beta: Double,
    )

    @Cacheable("beta")
    fun beta(ticker: String): Double? {
        // FIXME
        return rowCache[ticker]?.beta
    }

    @Cacheable("latestPrice")
    fun latestPrice(ticker: String): Double {
        // FIXME
        val row = rowCache[ticker]
        if (row != null) {
            return row.price
        }
        val url =
            "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=$ticker&interval=5min&apikey=$apiKey"
        val json = httpClient.readJson(url)
        val timeSeriesDaily = json.get("Time Series (Daily)")
        val latestDate = latestDate(timeSeriesDaily)
        return timeSeriesDaily[latestDate.toString()]["4. close"].textValue().toDouble()
    }

    private fun latestDate(timeSeriesDaily: JsonNode) = timeSeriesDaily
        .fields()
        .asSequence()
        .map { LocalDate.parse(it.key) }
        .sortedDescending()
        .toList()
        .first()

}