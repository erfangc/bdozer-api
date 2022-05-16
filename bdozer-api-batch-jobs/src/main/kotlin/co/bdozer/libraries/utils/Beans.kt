package co.bdozer.libraries.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.HttpHost
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import java.net.URI
import java.net.http.HttpClient

object Beans {
    fun restHighLevelClient(): RestHighLevelClient {
        val elasticsearchEndpoint = System.getenv("ELASTICSEARCH_ENDPOINT") ?: "http://localhost:9200"
        val elasticsearchCredential = System.getenv("ELASTICSEARCH_CREDENTIAL") ?: ""

        val uri = URI.create(elasticsearchEndpoint)
        val httpHost = HttpHost(uri.host, uri.port, uri.scheme)
        val headers = arrayOf(BasicHeader("Authorization", "Basic $elasticsearchCredential"))

        val builder = RestClient
            .builder(httpHost)
            .setDefaultHeaders(headers)
        return RestHighLevelClient(builder)
    }

    fun httpClient(): HttpClient {
        return HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
    }
    
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

}