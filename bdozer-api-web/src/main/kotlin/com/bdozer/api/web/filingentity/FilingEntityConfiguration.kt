package com.bdozer.api.web.filingentity

import com.bdozer.api.filing.entity.FilingEntityManager
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilingEntityConfiguration {
    @Bean
    fun filingEntityManager(
        mongoDatabase: MongoDatabase,
        httpClient: HttpClient,
    ): FilingEntityManager {
        return FilingEntityManager(mongoDatabase, httpClient)
    }
}