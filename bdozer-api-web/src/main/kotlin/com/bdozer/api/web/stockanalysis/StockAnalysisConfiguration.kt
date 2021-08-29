package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.support.DerivedAnalyticsComputer
import com.bdozer.api.stockanalysis.support.ModelEvaluator
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun stockAnalysisService(
        mongoDatabase: MongoDatabase,
        httpClient: HttpClient,
    ): StockAnalysisService {
        return StockAnalysisService(
            mongoDatabase = mongoDatabase,
            modelEvaluator = ModelEvaluator(
                httpClient = httpClient
            )
        )
    }

}