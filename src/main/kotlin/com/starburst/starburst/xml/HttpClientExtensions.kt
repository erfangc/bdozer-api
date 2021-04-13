package com.starburst.starburst.xml

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.http.HttpHeaders
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.function.Function
import javax.xml.parsers.DocumentBuilderFactory

object HttpClientExtensions {

    private val log = LoggerFactory.getLogger(HttpClientExtensions::class.java)
    private val cache = Caffeine.newBuilder().maximumSize(10_000)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .build<String, ByteArray>()

    fun InputStream.readXml(): XmlElement {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return XmlElement(builder.parse(this).documentElement)
    }

    private fun HttpClient.readLink(link: String): ByteArray? {
        return cache.get(link) { link ->
            log.info("Reading link $link")
            val get = HttpGet(link)
            get.addHeader(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36"
            )
            val httpResponse = this.execute(get)
            val entity = httpResponse.entity
            val allBytes = entity.content.readAllBytes()
            get.releaseConnection()
            allBytes
        }
    }

    fun HttpClient.readXml(link: String): XmlElement {
        val inputStream = this.readLink(link)?.inputStream() ?: error("$link not found")
        return inputStream.readXml()
    }

    inline fun <reified T> HttpClient.readEntity(link: String): T {
        val objectMapper = jacksonObjectMapper()
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        val get = HttpGet(link)
        get.addHeader(
            HttpHeaders.USER_AGENT,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_2_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36"
        )
        val response = this.execute(get)
        if (response.statusLine.statusCode in 200..299) {
            val ret = objectMapper.readValue<T>(response.entity.content)
            get.releaseConnection()
            return ret
        } else {
            val responseBody = response.entity.content.bufferedReader().readText()
            error("Error calling $link. status=${response.statusLine.statusCode} responseBody=$responseBody")
        }

    }

}
