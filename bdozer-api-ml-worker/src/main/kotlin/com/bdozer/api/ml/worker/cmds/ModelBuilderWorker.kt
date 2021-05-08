package com.bdozer.api.ml.worker.cmds

import com.bdozer.api.factbase.core.dataclasses.ProcessSECFilingRequest
import com.bdozer.api.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.factbase.modelbuilder.issues.IssueGenerator
import com.bdozer.api.factbase.modelbuilder.issues.IssueManager
import com.bdozer.api.filing.entity.FilingEntityManager
import com.bdozer.api.stockanalysis.StockAnalysisService
import com.bdozer.api.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import org.slf4j.LoggerFactory

class ModelBuilderWorker(
    private val channel: Channel,
    private val filingEntityManager: FilingEntityManager,
    private val stockAnalysisService: StockAnalysisService,
    private val modelBuilderFactory: ModelBuilderFactory,
    private val issueManager: IssueManager,
) : DeliverCallback {

    private val log = LoggerFactory.getLogger(BdozerMLWorker::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val issueGenerator = IssueGenerator()

    override fun handle(consumerTag: String, message: Delivery) {
        /*
        This is the main processing loop logic for every event this worker receives
        parse the request from binaries off the message queue
         */
        val deliveryTag = message.envelope.deliveryTag
        val request = try {
            objectMapper.readValue<ProcessSECFilingRequest>(message.body)
        } catch (e: Exception) {
            log.error(
                "Unable to deserialize message " +
                        "deliveryTag=$deliveryTag, " +
                        "consumerTag=$consumerTag, " +
                        "message=${e.message}", e
            )
            channel.basicAck(deliveryTag, false)
            return
        }

        val cik = request.cik
        val adsh = request.adsh
        val stockAnalysisId = "automated_$cik"

        /*
        Attempt to process the request
         */
        try {
            val filingEntity = filingEntityManager.getFilingEntity(cik) ?: filingEntityManager.createFilingEntity(cik)
            val savedStockAnalysis = stockAnalysisService.getStockAnalysis(stockAnalysisId)
            if (savedStockAnalysis == null) {
                /*
                create a new model + stock analysis
                 */
                val model = modelBuilderFactory.bestEffortModel(
                    cik = cik,
                    adsh = adsh
                )
                try {
                    val resp = stockAnalysisService.evaluateStockAnalysis(EvaluateModelRequest(model))
                    val stockAnalysis = StockAnalysis2(
                        _id = stockAnalysisId,
                        cik = cik,
                        cells = resp.cells,
                        ticker = filingEntity.tradingSymbol,
                        name = filingEntity.name,
                        derivedStockAnalytics = resp.derivedStockAnalytics,
                        model = resp.model.copy(adsh = adsh),
                        tags = listOf("RS3000", "Automated")
                    )
                    rerunIssues(stockAnalysis)
                    stockAnalysisService.saveStockAnalysis(stockAnalysis)
                } catch (e: Exception) {
                    stockAnalysisService.saveStockAnalysis(
                        StockAnalysis2(
                            _id = stockAnalysisId,
                            cik = cik,
                            cells = emptyList(),
                            ticker = filingEntity.tradingSymbol,
                            name = filingEntity.name,
                            derivedStockAnalytics = null,
                            model = model,
                            tags = listOf("RS3000", "Automated")
                        )
                    )
                }
                log.info("Created stock analysis $stockAnalysisId")
            } else {
                /*
                create a new model + stock analysis
                 */
                val model = modelBuilderFactory.bestEffortModel(
                    cik = cik,
                    adsh = adsh
                )
                val resp = stockAnalysisService.evaluateStockAnalysis(EvaluateModelRequest(model))
                /*
                Just update the analysis without creating new ones
                 */
                val stockAnalysis2 = savedStockAnalysis.copy(
                    cells = resp.cells,
                    derivedStockAnalytics = resp.derivedStockAnalytics,
                    model = savedStockAnalysis.model.copy(
                        incomeStatementItems = resp.model.incomeStatementItems,
                        cashFlowStatementItems = resp.model.cashFlowStatementItems,
                        balanceSheetItems = resp.model.balanceSheetItems,
                        otherItems = resp.model.otherItems,
                    ),
                )
                rerunIssues(stockAnalysis2)
                stockAnalysisService.saveStockAnalysis(stockAnalysis2)
                log.info("Updated stock analysis $stockAnalysisId")
            }
        } catch (e: Exception) {
            log.error("Unable to create or update stock analysis $stockAnalysisId, message=${e.message}")
        } finally {
            channel.basicAck(deliveryTag, false)
        }
    }

    private fun rerunIssues(stockAnalysis: StockAnalysis2) {
        issueManager.deleteAllIssues(stockAnalysis._id)
        val issues = issueGenerator.generateIssues(stockAnalysis = stockAnalysis)
        if (issues.isNotEmpty()) {
            issueManager.saveIssues(issues)
        }
    }
}