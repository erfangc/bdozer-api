package com.bdozer.api.ml.worker.cmds

import com.bdozer.api.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.filing.entity.FilingEntityManager
import com.bdozer.api.stockanalysis.iex.IEXService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rabbitmq.client.CancelCallback
import org.slf4j.LoggerFactory

class BdozerMLWorker

private const val QUEUE_NAME = "MODELS_TO_UPSERT"
private val log = LoggerFactory.getLogger(BdozerMLWorker::class.java)

fun main() {

    val cfg = AppConfiguration()

    /*
    Domain object construction code
     */
    val httpClient = cfg.httpClient()
    val mongoClient = cfg.mongoClient()
    val mongoDatabase = cfg.mongoDatabase(mongoClient)

    val stockAnalysisService = cfg.stockAnalysisService(
        iexService = IEXService(
            iexCloudClient = cfg.iexCloudClient(),
        ),
        mongoDatabase = mongoDatabase,
    )
    val modelBuilderFactory = ModelBuilderFactory(secFilingFactory = cfg.secFilingFactory(httpClient = httpClient))
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
    Start the model builder consumer
    to listen to messages and build models
     */
    val modelBuilderWorker = ModelBuilderWorker(
        filingEntityManager = filingEntityManager,
        stockAnalysisService = stockAnalysisService,
        channel = channel,
        modelBuilderFactory = modelBuilderFactory,
    )

    channel.basicConsume(QUEUE_NAME, false, modelBuilderWorker, CancelCallback { })
    log.info("Worker started and listening to messages")
}
