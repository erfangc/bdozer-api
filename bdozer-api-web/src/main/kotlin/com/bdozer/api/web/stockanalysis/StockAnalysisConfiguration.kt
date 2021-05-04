package com.bdozer.api.web.stockanalysis

import com.bdozer.api.factbase.core.SECFilingFactory
import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.iex.IEXService
import com.bdozer.api.stockanalysis.support.DerivedAnalyticsComputer
import com.bdozer.api.stockanalysis.support.StatelessModelEvaluator
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.zankowski.iextrading4j.client.IEXCloudClient

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun stockAnalysisService(mongoDatabase: MongoDatabase, iexService: IEXService): StockAnalysisService {
        return StockAnalysisService(
            mongoDatabase = mongoDatabase,
            statelessModelEvaluator = StatelessModelEvaluator(
                derivedAnalyticsComputer = DerivedAnalyticsComputer(
                    iexService = iexService
                )
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