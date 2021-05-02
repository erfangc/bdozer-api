package com.bdozer.api.web.factbase.cron

import com.bdozer.api.factbase.core.dataclasses.ProcessSECFilingRequest
import com.bdozer.api.factbase.core.extensions.HttpClientExtensions.readXml
import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.ConnectionFactory
import org.apache.http.client.HttpClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SECFilingCronJobs(
    connectionFactory: ConnectionFactory,
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(SECFilingCronJobs::class.java)

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

    @Scheduled(cron = "0 0 9-17 * * MON-FRI")
    fun processRss() {
        val url = "https://www.sec.gov/Archives/edgar/usgaap.rss.xml"
        val rssFeed = httpClient.readXml(url)
        rssFeed
            .getElementByTag("channel")
            ?.getElementsByTag("item")
            ?.forEach { xmlNode ->

                val xbrlFiling = xmlNode.getElementByTag("edgar:xbrlFiling")
                val formType = xbrlFiling?.getElementByTag("edgar:formType")?.textContent
                val cik = xbrlFiling?.getElementByTag("edgar:cikNumber")?.textContent
                val adsh = xbrlFiling?.getElementByTag("edgar:accessionNumber")?.textContent
                val isAcceptedFormType = formType == "10-K" || formType == "10-Q"

                if (isAcceptedFormType && adsh != null && cik != null) {
                    val request = ProcessSECFilingRequest(cik = cik, adsh = adsh)
                    channel.basicPublish(
                        "",
                        QUEUE_NAME,
                        null,
                        objectMapper.writeValueAsBytes(request),
                    )
                    log.info("Sent RSS item to MQ queue $QUEUE_NAME for processing, cik=$cik adsh=$adsh, formType=$formType")
                } else {
                    log.info("Skipping sending RSS item to MQ queue $QUEUE_NAME for processing, cik=$cik adsh=$adsh, formType=$formType")
                }
            }
    }

}