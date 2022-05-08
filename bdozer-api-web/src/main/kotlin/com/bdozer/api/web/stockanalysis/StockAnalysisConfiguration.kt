package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.support.ModelEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun stockAnalysisService(
        restHighLevelClient: RestHighLevelClient,
        httpClient: HttpClient,
        objectMapper: ObjectMapper,
    ): StockAnalysisService {
        return StockAnalysisService(
            restHighLevelClient = restHighLevelClient,
            modelEvaluator = ModelEvaluator(
                httpClient = httpClient
            ),
            objectMapper = objectMapper,
        )
    }

}