package com.bdozer.api.ml.worker.cmds

import com.bdozer.api.factbase.core.dataclasses.ProcessSECFilingRequest
import com.bdozer.api.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.filing.entity.FilingEntityManager
import com.bdozer.api.stockanalysis.dataclasses.EvaluateModelRequest
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import com.bdozer.api.stockanalysis.iex.IEXService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import org.slf4j.LoggerFactory

class BdozerMLWorker

private const val QUEUE_NAME = "MODELS_TO_UPSERT"

private val log = LoggerFactory.getLogger(BdozerMLWorker::class.java)

fun main(args: Array<String>) {

    val cfg = AppConfiguration()

    /*
    Domain object construction code
     */
    val httpClient = cfg.httpClient()
    val mongoClient = cfg.mongoClient()
    val mongoDatabase = cfg.mongoDatabase(mongoClient)
    val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    val iexCloudClient = cfg.iexCloudClient(mongoClient)
    val stockAnalysisService = cfg.stockAnalysisService(
        iexService = IEXService(
            iexCloudClient = iexCloudClient
        ),
        mongoDatabase = mongoDatabase
    )
    val secFilingFactory = cfg.secFilingFactory(httpClient = httpClient)
    val modelBuilderFactory = ModelBuilderFactory(secFilingFactory = secFilingFactory)
    val filingEntityManager = FilingEntityManager(mongoDatabase = mongoDatabase, httpClient = httpClient)

    /*
    AMQP infrastructure init code
     */
    val connectionFactory = cfg.connectionFactory()
    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()

    /*
    Prefetch set to 1 to avoid imbalance in processing
     */
    channel.basicQos(1)

    /*
    MQ queue declaration is idempotent
     */
    channel.queueDeclare(
        QUEUE_NAME,
        // not durable
        false,
        // not-exclusive to this connection
        false,
        // do not auto delete queue
        false,
        null
    )

    /*
    Register shutdown hooks to safely stop the worker
     */
    Runtime.getRuntime().addShutdownHook(Thread {
        mongoClient.close()
        channel.close()
        connection.close()
    })

    /*
    This is the main processing loop logic for every event this worker receives
     */
    val deliverCallback = DeliverCallback { consumerTag, message ->

        /*
        parse the request from binaries off the message queue
         */
        val request = try {
            objectMapper.readValue<ProcessSECFilingRequest>(message?.body!!)
        } catch (e: Exception) {
            log.error(
                "Unable to deserialize message deliveryTag=${message.envelope.deliveryTag}, consumerTag=$consumerTag, message=${e.message}",
                e
            )
            channel.basicAck(message.envelope.deliveryTag, false)
            return@DeliverCallback
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
                val resp = stockAnalysisService.evaluateStockAnalysis(EvaluateModelRequest(model))
                stockAnalysisService.saveStockAnalysis(
                    StockAnalysis2(
                        _id = stockAnalysisId,
                        cells = resp.cells,
                        ticker = filingEntity.tradingSymbol,
                        name = filingEntity.name,
                        derivedStockAnalytics = resp.derivedStockAnalytics,
                        model = resp.model.copy(adsh = adsh),
                        tags = listOf("RS3000", "Automated")
                    )
                )
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
                stockAnalysisService.saveStockAnalysis(
                    savedStockAnalysis.copy(
                        cells = resp.cells,
                        derivedStockAnalytics = resp.derivedStockAnalytics,
                        model = savedStockAnalysis.model.copy(
                            incomeStatementItems = resp.model.incomeStatementItems,
                            cashFlowStatementItems = resp.model.cashFlowStatementItems,
                            balanceSheetItems = resp.model.balanceSheetItems,
                            otherItems = resp.model.otherItems,
                        ),
                    )
                )
                log.info("Updated stock analysis $stockAnalysisId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            channel.basicAck(message.envelope.deliveryTag,false)
        }
    }

    /*
    Start the consumer code
     */
    channel.basicConsume(QUEUE_NAME, false, deliverCallback, CancelCallback { })
    log.info("Worker started and listening to messages")
}
