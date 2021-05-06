package com.bdozer.api.ml.worker.cmds

import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.dataclasses.PriceUpdateRequest
import com.bdozer.api.stockanalysis.iex.IEXService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import org.slf4j.LoggerFactory

class PriceUpdateWorker(
    private val channel: Channel,
    private val iexService: IEXService,
    private val stockAnalysisService: StockAnalysisService,
) : DeliverCallback {

    private val log = LoggerFactory.getLogger(PriceUpdateWorker::class.java)
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    override fun handle(consumerTag: String, message: Delivery) {
        try {
            val req = objectMapper.readValue<PriceUpdateRequest>(message.body)
            val currentPrice = iexService.price(ticker = req.ticker)
            val stockAnalysis = stockAnalysisService.getStockAnalysis(req.stockAnalysisId)
            val derivedStockAnalytics = stockAnalysis?.derivedStockAnalytics
            if (derivedStockAnalytics != null && currentPrice != null) {
                stockAnalysisService.saveStockAnalysis(
                    stockAnalysis.copy(
                        derivedStockAnalytics = derivedStockAnalytics.copy(currentPrice = currentPrice)
                    )
                )
                log.info("Price update complete for ${req.stockAnalysisId}")
            }
        } catch (e: Exception) {
            log.error("Unable to update prices for message=${e.message}")
        } finally {
            channel.basicAck(message.envelope.deliveryTag, false)
        }
    }
}

