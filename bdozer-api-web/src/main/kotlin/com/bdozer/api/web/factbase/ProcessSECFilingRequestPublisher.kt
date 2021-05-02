package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.dataclasses.ProcessSECFilingRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProcessSECFilingRequestPublisher(
    connectionFactory: ConnectionFactory,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(ProcessSECFilingRequestPublisher::class.java)
    private final val QUEUE_NAME = "FILING_TO_PROCESS"
    private val connection = connectionFactory.newConnection()
    private val channel = connection.createChannel()

    init {
        /*
        Reminder MQ queue declaration is idempotent
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
            channel.close()
            connection.close()
        })

    }

    fun publishRequest(cik: String, adsh: String) {
        val body = objectMapper.writeValueAsBytes(ProcessSECFilingRequest(cik = cik, adsh = adsh))
        channel.basicPublish(
            "",
            QUEUE_NAME,
            null,
            body
        )
        log.info("Published filing for processing, cik=$cik adsh=$adsh")
    }
}