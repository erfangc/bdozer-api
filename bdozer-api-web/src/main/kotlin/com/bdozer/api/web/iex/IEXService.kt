package com.bdozer.api.web.iex

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import pl.zankowski.iextrading4j.client.IEXCloudClient
import pl.zankowski.iextrading4j.client.rest.request.stocks.PriceRequestBuilder
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.BatchMarketStocksRequestBuilder
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.BatchStocksType

@Service
class IEXService(
    private val iexCloudClient: IEXCloudClient
) {

    fun prices(tickers: List<String>): Map<String, Double> {
        return tickers.chunked(50).flatMap { chunk ->
            val request = BatchMarketStocksRequestBuilder()
                .addType(BatchStocksType.QUOTE)
                .withSymbols(chunk)
                .build()
            val result = iexCloudClient.executeRequest(request)
            result.entries.map { (ticker, value) ->
                ticker to value.quote.latestPrice.toDouble()
            }
        }.toMap()
    }

    @Cacheable("stockPrice")
    fun price(ticker: String? = null): Double? {
        if (ticker == null) {
            return null
        }
        return iexCloudClient
            .executeRequest(PriceRequestBuilder().withSymbol(ticker).build())
            .toDouble()
    }

}