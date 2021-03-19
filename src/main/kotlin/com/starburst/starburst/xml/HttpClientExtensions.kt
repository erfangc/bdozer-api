package com.starburst.starburst.xml

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object HttpClientExtensions {

    fun InputStream.readXml(): XmlElement {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return XmlElement(builder.parse(this).documentElement)
    }

    fun HttpClient.readLink(link: String): ByteArray? {
        val get = HttpGet(link)
        val httpResponse = this.execute(get)
        val entity = httpResponse.entity
        val allBytes = entity.content.readAllBytes()
        get.releaseConnection()
        return allBytes
    }

    fun HttpClient.readXml(link: String): XmlElement {
        val inputStream = this.readLink(link)?.inputStream() ?: error("$link not found")
        return inputStream.readXml()
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
