package com.starburst.starburst.iex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingApiVersion
import pl.zankowski.iextrading4j.client.IEXTradingClient
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.CashFlowRequestBuilder
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.Period

const val token = "sk_3eb90212a9df48af9b0c4a776fd24741"

fun main() {
    val cli = IEXTradingClient.create(
        IEXTradingApiVersion.IEX_CLOUD_STABLE,
        IEXCloudTokenBuilder()
            .withPublishableToken("pk_d66bdb23bae6444e85c16fbb4fff2e29")
            .withSecretToken(token)
            .build()
    )

    val get = cli.executeRequest(
        CashFlowRequestBuilder()
            .withSymbol("DBX")
            .withPeriod(Period.ANNUAL)
            .build()
    )
    get.cashFlow.forEach { cf ->
        println("""
            ${cf}
        """.trimIndent())
    }
}

private fun testIncomeStatements() {
    val http = HttpClientBuilder
        .create()
        .build()

    val get =
        HttpGet("$token")

    val bytes = http.execute(get).entity.content.readAllBytes()
    println(bytes.inputStream().bufferedReader().readText())
    val fs = jacksonObjectMapper().readValue<List<Fundamental>>(bytes)
    get.releaseConnection()

    fs.forEach { f ->
        println("expensesStockCompensation: ${f.expensesStockCompensation}")
    }
}