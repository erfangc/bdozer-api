package com.bdozer.api.web

import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.System.getenv
import java.net.URI

@Configuration
class AppConfiguration {

    @Bean
    fun httpClient(): HttpClient {
        return HttpClientBuilder.create().build()
    }

    @Bean
    fun restHighLevelClient(): RestHighLevelClient {
        val elasticsearchEndpoint = getenv("ELASTICSEARCH_ENDPOINT") ?: "http://localhost:9200"
        val elasticsearchCredential = getenv("ELASTICSEARCH_CREDENTIAL") ?: ""

        val uri = URI.create(elasticsearchEndpoint)
        val httpHost = HttpHost(uri.host, uri.port, uri.scheme)
        val headers = arrayOf(BasicHeader("Authorization", "Basic $elasticsearchCredential"))

        val builder = RestClient
            .builder(httpHost)
            .setDefaultHeaders(headers)
        return RestHighLevelClient(builder)
    }

}
