package com.bdozer.api.match

import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {

    @Bean
    fun restHighlevelClient(): RestHighLevelClient {
        return RestHighLevelClient(
            RestClient.builder(HttpHost("localhost", 9200))
        )
    }

    @Bean
    fun httpClient(): HttpClient {
        return HttpClientBuilder.create().build()
    }

}
