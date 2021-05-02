package com.bdozer.api.web.sec.factbase.ingestor.cron

import com.bdozer.api.web.filingentity.FilingEntityBootstrapper
import com.bdozer.api.web.filingentity.FilingEntityManager
import com.bdozer.api.web.sec.factbase.ingestor.FilingIngestor
import com.bdozer.api.web.sec.factbase.ingestor.Q4FactFinder
import com.bdozer.api.web.xml.HttpClientExtensions.readXml
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ofPattern

@Service
class SecIngestorCronJobs(
    private val httpClient: HttpClient,
    private val ingestor: FilingIngestor,
    private val filingEntityManager: FilingEntityManager,
    private val filingEntityBootstrapper: FilingEntityBootstrapper,
    mongoDatabase: MongoDatabase,
) {

    private val log = LoggerFactory.getLogger(SecIngestorCronJobs::class.java)
    private val col = mongoDatabase.getCollection<XbrlRssItem>()
    private val q4FactFinder = Q4FactFinder(mongoDatabase)

    @Scheduled(cron = "0 0 9-17 * * MON-FRI")
    fun processLatest() {
        val url = "https://www.sec.gov/Archives/edgar/usgaap.rss.xml"
        val xml = httpClient.readXml(url)
        val items = xml
            .getElementByTag("channel")
            ?.getElementsByTag("item")
            ?.map { xmlNode ->
                val xbrlFiling = xmlNode.getElementByTag("edgar:xbrlFiling")
                val companyName = xbrlFiling?.getElementByTag("edgar:companyName")?.textContent
                val formType = xbrlFiling?.getElementByTag("edgar:formType")?.textContent
                val cikNumber = xbrlFiling?.getElementByTag("edgar:cikNumber")?.textContent
                val accessionNumber = xbrlFiling?.getElementByTag("edgar:accessionNumber")?.textContent
                val period = xbrlFiling
                    ?.getElementByTag("edgar:period")
                    ?.textContent
                    ?.let { period ->
                        LocalDate.parse(period, ofPattern("yyyyMMdd"))
                    }
                /*
                XbrlRssItem
                 */
                XbrlRssItem(
                    _id = "$cikNumber$accessionNumber",
                    companyName = companyName,
                    formType = formType,
                    cikNumber = cikNumber!!,
                    accessionNumber = accessionNumber!!,
                    period = period,
                    status = Status.Pending,
                )
            }
            ?.filter { item -> item.formType == "10-K" || item.formType == "10-Q" } ?: emptyList()

        items.distinctBy { it.cikNumber }.map { item ->
            /*
            create the filing entity if it does not already exist
             */
            filingEntityManager.getFilingEntity(item.cikNumber)
                ?: filingEntityBootstrapper.createFilingEntity(item.cikNumber)
        }

        for (item in items) {
            try {
                val status = col.findOneById(item._id)?.status
                if (status != Status.Processed && status != Status.Error) {
                    col.save(item.copy(status = Status.Pending))
                    val accessionNumber = item.accessionNumber
                    val cikNumber = item.cikNumber
                    ingestor.ingestFiling(cik = cikNumber, adsh = accessionNumber)
                    col.save(item.copy(status = Status.Processed))
                } else {
                    log.info("Skipping processing cikNumber=${item.cikNumber}, item.accessionNumber=${item.accessionNumber}, status=${status}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                col.save(item.copy(status = Status.Error, message = e.message))
            }
        }
    }

    fun runForPastYears(numYearsToLookback: Int?) {

        val months = (1..12).map { it.toString().padStart(2, '0') }
        val currentYear = LocalDate.now().year
        val years = (1..(numYearsToLookback ?: 1)).map { currentYear - it }

        val items = years.flatMap { year ->
            months.flatMap { month ->
                val prefix = "https://www.sec.gov/Archives/edgar/monthly"
                val filename = "xbrlrss-$year-$month.xml"
                val url = "$prefix/$filename"
                val xml = httpClient.readXml(url)
                val items = xml
                    .getElementByTag("channel")
                    ?.getElementsByTag("item")
                    ?.map { xmlNode ->
                        val xbrlFiling = xmlNode.getElementByTag("edgar:xbrlFiling")
                        val companyName = xbrlFiling?.getElementByTag("edgar:companyName")?.textContent
                        val formType = xbrlFiling?.getElementByTag("edgar:formType")?.textContent
                        val cikNumber = xbrlFiling?.getElementByTag("edgar:cikNumber")?.textContent
                        val accessionNumber = xbrlFiling?.getElementByTag("edgar:accessionNumber")?.textContent
                        val period = xbrlFiling
                            ?.getElementByTag("edgar:period")
                            ?.textContent
                            ?.let { period ->
                                LocalDate.parse(period, ofPattern("yyyyMMdd"))
                            }
                        /*
                        XbrlRssItem
                         */
                        XbrlRssItem(
                            _id = "$cikNumber$accessionNumber",
                            companyName = companyName,
                            formType = formType,
                            cikNumber = cikNumber!!,
                            accessionNumber = accessionNumber!!,
                            period = period,
                            status = Status.Pending,
                        )
                    }
                    ?.filter { item -> item.formType == "10-K" || item.formType == "10-Q" } ?: emptyList()
                items
            }
        }

        items.distinctBy { it.cikNumber }.map { item ->
            /*
            create the filing entity if it does not already exist
             */
            filingEntityManager.getFilingEntity(item.cikNumber)
                ?: filingEntityBootstrapper.createFilingEntity(item.cikNumber)
        }

        for (item in items) {
            try {
                val status = col.findOneById(item._id)?.status
                if (status != Status.Processed) {
                    col.save(item.copy(status = Status.Pending))
                    val accessionNumber = item.accessionNumber
                    val cikNumber = item.cikNumber
                    ingestor.ingestFiling(cik = cikNumber, adsh = accessionNumber)
                    col.save(item.copy(status = Status.Processed))
                } else {
                    log.info("Skipping processing cikNumber=${item.cikNumber}, item.accessionNumber=${item.accessionNumber}, status=${status}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                col.save(item.copy(status = Status.Error, message = e.message))
            }
        }

        /*
        Fill in Q4
         */
        items
            .groupBy { item -> item.cikNumber }
            .forEach { (cik, items) ->
                items
                    .groupBy { it.period?.year }
                    .forEach { (year, items) ->
                        try {
                            if (items.size > 3) {
                                q4FactFinder.run(cik, year!!)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }
    }

}
