package com.starburst.starburst.xbrl.utils

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory

object HttpClientExtensions {

    private val log = LoggerFactory.getLogger(HttpClientExtensions::class.java)

    fun HttpClient.readLink(link: String): ByteArray? {
        log.info("Reading remote link $link")
        val get = HttpGet(link)
        val httpResponse = this.execute(get)
        val entity = httpResponse.entity
        val allBytes = entity.content.readAllBytes()
        get.releaseConnection()
        log.info("Done reading remote link $link")
        return allBytes
    }

}
