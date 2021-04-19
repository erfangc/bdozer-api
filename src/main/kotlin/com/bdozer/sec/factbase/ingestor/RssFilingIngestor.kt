package com.bdozer.sec.factbase.ingestor

import com.bdozer.filingentity.FilingEntityBootstrapper
import com.mongodb.client.MongoDatabase
import com.bdozer.filingentity.FilingEntityManager
import com.bdozer.xml.HttpClientExtensions.readXml
import org.apache.http.client.HttpClient
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class RssFilingIngestor(
    private val httpClient: HttpClient,
    private val ingestor: FilingIngestor,
    private val q4FactFinder: Q4FactFinder,
    private val filingEntityManager: FilingEntityManager,
    private val filingEntityBootstrapper: FilingEntityBootstrapper,
    mongoDatabase: MongoDatabase,
) {

    private val log = LoggerFactory.getLogger(RssFilingIngestor::class.java)
    private val col = mongoDatabase.getCollection<XbrlItem>()

    enum class Status {
        Processed, Pending, Error
    }

    data class XbrlItem(
        val _id: String,
        val companyName: String? = null,
        val formType: String? = null,
        val cikNumber: String,
        val accessionNumber: String,
        val period: LocalDate? = null,
        val status: Status,
        val message: String? = null,
    )

    private fun pad(input: Int, total: Int = 2): String {
        val charArray = input.toString().toCharArray()
        val newCharArray = CharArray(total)
        val start = newCharArray.size - charArray.size
        charArray.forEachIndexed { index, c ->
            newCharArray[start + index] = c
        }
        for (i in 0 until newCharArray.size - charArray.size) {
            newCharArray[i] = '0'
        }
        return String(newCharArray)
    }

    fun run(numYearsToLookback: Int?) {
        val months = (1..12).map { pad(it) }
        val currentYear = LocalDate.now().year
        val years = (1..(numYearsToLookback ?: 1)).map { currentYear - it }
        val items = years.flatMap { year ->
            months.flatMap { month ->
                val prefix = "https://www.sec.gov/Archives/edgar/monthly"
                val filename = "xbrlrss-$year-$month.xml"
                val url = "$prefix/$filename"
                log.info("Reading XBRL RSS $url")
                val xml = httpClient.readXml(url)
                val items = xml
                    .getElementByTag("channel")
                    ?.getElementsByTag("item")
                    ?.map { item ->
                        val xbrlFiling = item.getElementByTag("edgar:xbrlFiling")
                        val companyName = xbrlFiling?.getElementByTag("edgar:companyName")?.textContent
                        val formType = xbrlFiling?.getElementByTag("edgar:formType")?.textContent
                        val cikNumber = xbrlFiling?.getElementByTag("edgar:cikNumber")?.textContent
                        val accessionNumber = xbrlFiling?.getElementByTag("edgar:accessionNumber")?.textContent
                        val period = xbrlFiling?.getElementByTag("edgar:period")?.textContent?.let {
                            LocalDate.parse(
                                it,
                                DateTimeFormatter.ofPattern("yyyyMMdd")
                            )
                        }
                        val item = XbrlItem(
                            _id = "$cikNumber$accessionNumber",
                            companyName = companyName,
                            formType = formType,
                            cikNumber = cikNumber!!,
                            accessionNumber = accessionNumber!!,
                            period = period,
                            status = Status.Pending,
                        )
                        item
                    }
                    ?.filter { item -> item.formType == "10-K" || item.formType == "10-Q" } ?: emptyList()
                items
            }
        }

        items.distinctBy { it.cikNumber }.map { item ->
            // create the filing entity if it does not already exist
            filingEntityManager.getFilingEntity(item.cikNumber)
                ?: filingEntityBootstrapper.createFilingEntity(item.cikNumber)
        }

        for (item in items) {
            try {
                if (col.findOneById(item._id)?.status != Status.Processed) {
                    col.save(item.copy(status = Status.Pending))
                    val accessionNumber = item.accessionNumber
                    val cikNumber = item.cikNumber
                    ingestor.ingestFiling(cik = cikNumber, adsh = accessionNumber)
                    col.save(item.copy(status = Status.Processed))
                } else {
                    log.info("Skipping processing cikNumber=${item.cikNumber}, item.accessionNumber=${item.accessionNumber}, status=${item.status}")
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
