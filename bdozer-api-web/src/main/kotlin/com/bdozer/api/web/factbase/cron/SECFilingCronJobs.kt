package com.bdozer.api.web.factbase.cron

import com.bdozer.api.factbase.core.extensions.HttpClientExtensions.readXml
import com.bdozer.api.web.factbase.ProcessSECFilingRequestPublisher
import org.apache.http.client.HttpClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SECFilingCronJobs(
    private val processSECFilingRequestPublisher: ProcessSECFilingRequestPublisher,
    private val httpClient: HttpClient
) {

    private val log = LoggerFactory.getLogger(SECFilingCronJobs::class.java)

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
                    processSECFilingRequestPublisher.publishRequest(cik = cik, adsh = adsh)
                } else {
                    log.info("Skipping publishing RSS item for processing, cik=$cik adsh=$adsh, formType=$formType")
                }
            }
    }

}