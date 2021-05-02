package com.bdozer.api.factbase

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import org.slf4j.LoggerFactory

class BdozerFactbaseApplication

private const val QUEUE_NAME = "FILING_TO_PROCESS"

private val log = LoggerFactory.getLogger(BdozerFactbaseApplication::class.java)

fun main(args: Array<String>) {

    val cfg = AppConfiguration()

    /*
    Domain object construction code
     */
    val httpClient = cfg.httpClient()
    val secFilingFactory = cfg.secFilingFactory(httpClient)
    val mongoClient = cfg.mongoClient()
    val mongoDatabase = cfg.mongoDatabase(mongoClient)
    val filingIngestor = FilingIngestor(mongoDatabase, secFilingFactory)

    /*
    AMQP infrastructure init code
     */
    val connectionFactory = cfg.connectionFactory()
    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()

    /*
    Reminder MQ queue declaration is idempotent
     */
    channel.queueDeclare(
        QUEUE_NAME,
        // durable
        true,
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

    val deliverCallback = DeliverCallback { _, message ->
        // TODO implement business logic here
        log.info(message?.body?.decodeToString())

        /*
        ack the message ONLY after the work is complete, explicit ack is enabled
        this prevents too many messages from being processed by a given worker
         */
        channel.basicAck(message.envelope.deliveryTag, false)
    }

    /*
    Start the consumer code
     */
    channel.basicConsume(QUEUE_NAME, false, deliverCallback, CancelCallback {  })
}
