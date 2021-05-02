package com.bdozer.api.factbase

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import org.slf4j.LoggerFactory

class BdozerFactbaseApplication

private const val QUEUE_NAME = "FILING_TO_PROCESS"

private val log = LoggerFactory.getLogger(BdozerFactbaseApplication::class.java)

fun main(args: Array<String>) {

    val cfg = AppConfiguration()
    val httpClient = cfg.httpClient()
    val secFilingFactory = cfg.secFilingFactory(httpClient)
    val mongoClient = cfg.mongoClient()
    val mongoDatabase = cfg.mongoDatabase(mongoClient)
    val filingIngestor = FilingIngestor(mongoDatabase, secFilingFactory)
    val connectionFactory = cfg.connectionFactory()
    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()

    Runtime.getRuntime().addShutdownHook(Thread {
        mongoClient.close()
        channel.close()
        connection.close()
    })

    channel.queueDeclare(
        QUEUE_NAME,
        false,
        false,
        false,
        null
    )

    val deliverCallback = DeliverCallback { _, message ->
        log.info(message?.body?.decodeToString())
    }
    channel.basicConsume(QUEUE_NAME, true, deliverCallback, CancelCallback {  })
}
