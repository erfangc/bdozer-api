package com.starburst.starburst.edgar.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.starburst.starburst.xml.XbrlUtils
import com.starburst.starburst.xml.XmlElement
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory

object HttpClientExtensions {

    private val log = LoggerFactory.getLogger(HttpClientExtensions::class.java)

    fun HttpClient.readLink(link: String): ByteArray? {
        val get = HttpGet(link)
        val httpResponse = this.execute(get)
        val entity = httpResponse.entity
        val allBytes = entity.content.readAllBytes()
        get.releaseConnection()
        log.info("Read remote link $link")
        return allBytes
    }

    fun HttpClient.readXml(link: String): XmlElement {
        val inputStream = this.readLink(link)?.inputStream() ?: error("$link not found")
        return XbrlUtils.readXml(inputStream)
    }

    inline fun <reified T> HttpClient.readEntity(link: String): T {
        val objectMapper = jacksonObjectMapper()
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        val get = HttpGet(link)
        val execute = this.execute(get)
        val ret = objectMapper.readValue<T>(execute.entity.content)
        get.releaseConnection()
        return ret
    }

}
