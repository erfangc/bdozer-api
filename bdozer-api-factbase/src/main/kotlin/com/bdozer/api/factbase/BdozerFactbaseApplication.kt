package com.bdozer.api.factbase

import com.bdozer.api.factbase.core.dataclasses.ProcessSECFilingRequest
import com.bdozer.api.factbase.core.dataclasses.ProcessedSECFiling
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import java.time.Instant

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
    val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    val filingIngestor = FilingIngestor(mongoDatabase, secFilingFactory)
    val processedSECFilings = mongoDatabase.getCollection<ProcessedSECFiling>()

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

        /*
        Attempt to process the request
         */
        val normalizedAdsh = request.adsh.replace("-", "")
        try {
            val processedSECFiling = processedSECFilings.findOneById(normalizedAdsh)
            if (processedSECFiling == null) {
                /*
                The request has not been processed before
                 */
                val response = filingIngestor.ingestFiling(request.cik, normalizedAdsh)
                processedSECFilings.save(
                    ProcessedSECFiling(
                        _id = normalizedAdsh,
                        cik = request.cik,
                        adsh = normalizedAdsh,
                        numberOfFactsFound = response.numberOfFactsFound,
                        documentFiscalYearFocus = response.documentFiscalYearFocus,
                        documentPeriodEndDate = response.documentPeriodEndDate,
                        documentFiscalPeriodFocus = response.documentFiscalPeriodFocus,
                        timestamp = Instant.now()
                    )
                )
            } else {
                /*
                Skip processing the request
                 */
                log.info("Skipping the processing of adsh=$normalizedAdsh, cik=${request.cik}, it's already been processed")
            }
        } catch (e: Exception) {
            /*
            Error encountered while processing request
             */
            processedSECFilings.save(
                ProcessedSECFiling(
                    _id = normalizedAdsh,
                    cik = request.cik,
                    adsh = normalizedAdsh,
                    timestamp = Instant.now(),
                    error = e.message,
                )
            )
        } finally {
            /*
            ack the message ONLY after the work is complete, explicit ack is enabled
            this prevents too many messages from being processed by a given worker

            while processing of SEC filings is idempotent, processing failure
            count as message processed, as we cannot know in advance
            whether the failure will be indefinitely repeated or not if added back onto the message
            queue
            */
            channel.basicAck(message.envelope.deliveryTag, false)
        }
    }

    /*
    Start the consumer code
     */
    channel.basicConsume(QUEUE_NAME, false, deliverCallback, CancelCallback { })
    log.info("Worker started and listening to messages")
}
