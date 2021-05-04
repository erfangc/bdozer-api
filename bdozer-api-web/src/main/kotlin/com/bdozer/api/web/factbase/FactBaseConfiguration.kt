package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.SECFilingFactory
import com.bdozer.api.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.factbase.modelbuilder.issues.IssueManager
import com.mongodb.client.MongoDatabase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FactBaseConfiguration {
    @Bean
    fun modelBuilderFactory(secFilingFactory: SECFilingFactory): ModelBuilderFactory {
        return ModelBuilderFactory(
            secFilingFactory = secFilingFactory
        )
    }
    @Bean
    fun issueManager(mongoDatabase: MongoDatabase): IssueManager {
        return IssueManager(mongoDatabase = mongoDatabase)
    }
}