package com.starburst.starburst.extensions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet

object JsonExtensions {
    val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    inline fun <reified T> HttpClient.readValue(url: String): T {
        val get = HttpGet(url)
        val resp = execute(get)
        val ret = objectMapper.readValue<T>(resp.entity.content)
        get.releaseConnection()
        return ret
    }

    fun HttpClient.readJson(url: String): JsonNode {
        val get = HttpGet(url)
        val resp = execute(get)
        val ret = objectMapper.readTree(resp.entity.content)
        get.releaseConnection()
        return ret
    }
}