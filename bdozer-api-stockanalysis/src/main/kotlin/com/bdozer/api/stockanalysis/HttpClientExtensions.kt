package com.bdozer.api.stockanalysis

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.HttpHeaders
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet

object HttpClientExtensions {

    inline fun <reified T> HttpClient.readEntity(link: String): T {
        val objectMapper = jacksonObjectMapper()
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val get = HttpGet(link)
        get.addHeader(
            HttpHeaders.USER_AGENT,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36",
        )
        val response = this.execute(get)
        if (response.statusLine.statusCode in 200..299) {
            val ret = objectMapper.readValue<T>(response.entity.content)
            get.releaseConnection()
            return ret
        } else {
            val responseBody = response.entity.content.bufferedReader().readText()
            get.releaseConnection()
            error("Error calling $link. status=${response.statusLine.statusCode} responseBody=$responseBody")
        }
    }

}