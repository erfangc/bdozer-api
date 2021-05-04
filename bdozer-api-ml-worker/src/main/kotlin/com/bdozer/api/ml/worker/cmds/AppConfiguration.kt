package com.bdozer.api.ml.worker.cmds

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.rabbitmq.client.ConnectionFactory
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.litote.kmongo.KMongo
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
    fun connectionFactory(): ConnectionFactory {
        val rabbitMqUrl = URI(getenv("CLOUDAMQP_URL") ?: error("environment CLOUDAMQP_URL not defined"))
        val path = rabbitMqUrl.path
        val userInfo = rabbitMqUrl.userInfo?.split(":")
        val username = if (userInfo?.isNotEmpty() == true) userInfo[0] else null
        val password = if (userInfo != null && userInfo.size > 1) userInfo[1] else null
        val host = rabbitMqUrl.host
        val port = rabbitMqUrl.port
        val virtualHost = if (path != null && path.length > 1) path.substring(1) else null
        val connectionFactory = ConnectionFactory()
        if (username != null) {
            connectionFactory.username = username
        }
        if (password != null) {
            connectionFactory.password = password
        }
        if (host != null) {
            connectionFactory.host = host
        }
        if (port != null) {
            connectionFactory.port = port
        }
        if (virtualHost != null) {
            connectionFactory.virtualHost = virtualHost
        }
        return connectionFactory
    }
}
