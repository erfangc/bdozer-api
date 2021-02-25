package com.starburst.starburst

import com.mongodb.client.MongoClient
import org.litote.kmongo.KMongo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.System.getenv

@Configuration
class MongoConfiguration {
    @Bean
    fun mongo(): MongoClient {
        val connectionString = getenv("MONGO_URI")
        return KMongo.createClient(connectionString)
    }
}
