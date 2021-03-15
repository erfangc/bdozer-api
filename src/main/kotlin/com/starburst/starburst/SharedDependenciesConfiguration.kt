package com.starburst.starburst

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.litote.kmongo.KMongo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.System.getenv

@Configuration
class SharedDependenciesConfiguration {
    @Bean
    fun httpClient(): HttpClient {
        return HttpClientBuilder.create().build()
    }

    @Bean
    fun mongoClient(): MongoClient {
        val connectionString = getenv("MONGO_URI")
        return KMongo.createClient(connectionString)
    }

    @Bean
    fun mongoDatabase(mongoClient: MongoClient): MongoDatabase {
        return mongoClient.getDatabase("starburst")
    }
}
