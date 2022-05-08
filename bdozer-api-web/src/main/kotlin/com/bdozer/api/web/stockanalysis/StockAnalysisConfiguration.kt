package com.bdozer.api.web.stockanalysis

import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.support.ModelEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class StockAnalysisConfiguration {

    @Bean
    fun stockAnalysisService(
        restHighLevelClient: RestHighLevelClient,
        httpClient: HttpClient,
        objectMapper: ObjectMapper,
        s3: S3Client,
    ): StockAnalysisService {
        return StockAnalysisService(
            restHighLevelClient = restHighLevelClient,
            objectMapper = objectMapper,
            s3 = s3,
        )
    }

}