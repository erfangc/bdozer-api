package com.bdozer.api.ml.worker.cmds

import com.bdozer.api.factbase.modelbuilder.ModelBuilderFactory
import com.bdozer.api.filing.entity.FilingEntityManager
import com.bdozer.api.stockanalysis.iex.IEXService
import com.rabbitmq.client.CancelCallback
import org.slf4j.LoggerFactory

class BdozerMLWorker

private const val MODELS_TO_UPSERT = "MODELS_TO_UPSERT"
private const val PRICES_TO_UPDATE = "PRICES_TO_UPDATE"

private val log = LoggerFactory.getLogger(BdozerMLWorker::class.java)

fun main() {

    val cfg = AppConfiguration()

    /*
    Domain object construction code
     */
    val httpClient = cfg.httpClient()
    val mongoClient = cfg.mongoClient()
    val mongoDatabase = cfg.mongoDatabase(mongoClient)

    val iexService = IEXService(
        iexCloudClient = cfg.iexCloudClient(),
    )
    val stockAnalysisService = cfg.stockAnalysisService(
        iexService = iexService,
        mongoDatabase = mongoDatabase,
    )
    val modelBuilderFactory = ModelBuilderFactory(
        secFilingFactory = cfg.secFilingFactory(httpClient = httpClient)
    )
    val filingEntityManager = FilingEntityManager(
        mongoDatabase = mongoDatabase, httpClient = httpClient
    )

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
        MODELS_TO_UPSERT,
        // Not durable
        false,
        // Not-exclusive to this connection
        false,
        // Do not auto delete queue
        false,
        null,
    )

    channel.queueDeclare(
        PRICES_TO_UPDATE,
        // Not durable
        false,
        // Not-exclusive to this connection
        false,
        // Do not auto delete queue
        false,
        null,
    )

    /*
    Register shutdown hooks to safely stop the worker
     */
    Runtime
        .getRuntime()
        .addShutdownHook(
            Thread {
                mongoClient.close()
                channel.close()
                connection.close()
            }
        )

    /*
    Start the model builder consumer
    to listen to messages and build models
     */
    val modelBuilderWorker = ModelBuilderWorker(
        filingEntityManager = filingEntityManager,
        stockAnalysisService = stockAnalysisService,
        modelBuilderFactory = modelBuilderFactory,
        channel = channel,
    )
    val priceUpdateWorker = PriceUpdateWorker(
        channel = channel,
        iexService = iexService,
        stockAnalysisService = stockAnalysisService,
    )

    channel.basicConsume(MODELS_TO_UPSERT, false, modelBuilderWorker, CancelCallback { })
    channel.basicConsume(PRICES_TO_UPDATE, false, priceUpdateWorker, CancelCallback { })

    log.info("Worker started and listening to messages")
}
