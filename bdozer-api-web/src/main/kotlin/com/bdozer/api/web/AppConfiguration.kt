package com.bdozer.api.web

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.litote.kmongo.KMongo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.zankowski.iextrading4j.client.IEXCloudClient
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingClient
import java.lang.System.getenv

@Configuration
class AppConfiguration {

    @Bean
    fun httpClient(): HttpClient {
        return HttpClientBuilder.create().build()
    }

    @Bean
    fun mongoClient(): MongoClient {
        val connectionString = getenv("MONGO_URI")
            ?: error("environment MONGO_URI not defined")
        return KMongo.createClient(connectionString)
    }

    @Bean
    fun mongoDatabase(mongoClient: MongoClient): MongoDatabase {
        val database = getenv("MONGO_DATABASE")
            ?: error("environment MONGO_DATABASE not defined")
        return mongoClient.getDatabase(database)
    }

    @Bean
    fun iexCloudClient(mongoClient: MongoClient): IEXCloudClient {

        val token = IEXCloudTokenBuilder()
            .withPublishableToken("pk_d66bdb23bae6444e85c16fbb4fff2e29")
            .withSecretToken(getenv("IEX_SECRET_TOKEN") ?: error("environment IEX_SECRET_TOKEN not defined"))
            .build()

        return IEXTradingClient.create(token)
    }
}
