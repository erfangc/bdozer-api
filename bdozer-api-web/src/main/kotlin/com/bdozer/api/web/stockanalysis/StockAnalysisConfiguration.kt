package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.iex.IEXService
import com.bdozer.api.stockanalysis.support.DerivedAnalyticsComputer
import com.bdozer.api.stockanalysis.support.StatelessModelEvaluator
import com.mongodb.client.MongoDatabase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingClient

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun derivedAnalyticsComputer(iexService: IEXService): DerivedAnalyticsComputer {
        return DerivedAnalyticsComputer(iexService)
    }

    @Bean
    fun stockAnalysisService(
        mongoDatabase: MongoDatabase,
        derivedAnalyticsComputer: DerivedAnalyticsComputer
    ): StockAnalysisService {
        return StockAnalysisService(
            mongoDatabase = mongoDatabase,
            statelessModelEvaluator = StatelessModelEvaluator(
                derivedAnalyticsComputer = derivedAnalyticsComputer
            )
        )
    }

    @Bean
    fun iexService(): IEXService {
        val token = IEXCloudTokenBuilder()
            .withPublishableToken("pk_d66bdb23bae6444e85c16fbb4fff2e29")
            .withSecretToken(System.getenv("IEX_SECRET_TOKEN") ?: error("environment IEX_SECRET_TOKEN not defined"))
            .build()
        val iexCloudClient = IEXTradingClient.create(token)
        return IEXService(iexCloudClient = iexCloudClient)
    }

}