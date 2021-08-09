package com.bdozer.api.web.stockanalysis

import com.bdozer.api.factbase.core.SECFilingFactory
import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.iex.IEXService
import com.bdozer.api.stockanalysis.kpis.dataclasses.CompanyKPIsService
import com.bdozer.api.stockanalysis.support.DerivedAnalyticsComputer
import com.bdozer.api.stockanalysis.support.StatelessModelEvaluator
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.zankowski.iextrading4j.client.IEXCloudClient
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder
import pl.zankowski.iextrading4j.client.IEXTradingClient

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun iexCloudClient(mongoClient: MongoClient): IEXCloudClient {
        val token = IEXCloudTokenBuilder()
            .withPublishableToken("pk_d66bdb23bae6444e85c16fbb4fff2e29")
            .withSecretToken(System.getenv("IEX_SECRET_TOKEN") ?: error("environment IEX_SECRET_TOKEN not defined"))
            .build()
        return IEXTradingClient.create(token)
    }

    @Bean
    fun service(mongoDatabase: MongoDatabase): CompanyKPIsService {
        return CompanyKPIsService(mongoDatabase)
    }

    @Bean
    fun derivedAnalyticsComputer(iexService: IEXService): DerivedAnalyticsComputer {
        return DerivedAnalyticsComputer(iexService)
    }

    @Bean
    fun stockAnalysisService(mongoDatabase: MongoDatabase, derivedAnalyticsComputer: DerivedAnalyticsComputer): StockAnalysisService {
        return StockAnalysisService(
            mongoDatabase = mongoDatabase,
            statelessModelEvaluator = StatelessModelEvaluator(
                derivedAnalyticsComputer = derivedAnalyticsComputer
            )
        )
    }

    @Bean
    fun iexService(iexCloudClient: IEXCloudClient): IEXService {
        return IEXService(iexCloudClient = iexCloudClient)
    }

    @Bean
    fun secFilingFactory(httpClient: HttpClient): SECFilingFactory {
        return SECFilingFactory(httpClient)
    }

}