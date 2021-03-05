package com.starburst.starburst.edgar.utils

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.dataclasses.XbrlUtils
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
        return XmlElement(XbrlUtils.readXml(inputStream))
    }

}
