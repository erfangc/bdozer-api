package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.factbase.ingestor.q4.Q4FactFinder
import com.starburst.starburst.xml.HttpClientExtensions.readXml
import org.apache.http.client.HttpClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class RssFilingIngestor(
    private val httpClient: HttpClient,
    private val ingestor: FilingIngestor,
    private val q4FactFinder: Q4FactFinder,
) {

    internal data class XbrlItem(
        val companyName: String? = null,
        val formType: String? = null,
        val cikNumber: String? = null,
        val accessionNumber: String? = null,
        val period: LocalDate? = null,
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
                        XbrlItem(
                            companyName = companyName,
                            formType = formType,
                            cikNumber = cikNumber,
                            accessionNumber = accessionNumber,
                            period = period,
                        )
                    }
                    ?.filter { item -> item.formType == "10-K" || item.formType == "10-Q" } ?: emptyList()
                items
            }
        }

        for (item in items) {
            try {
                val accessionNumber = item.accessionNumber
                val cikNumber = item.cikNumber
                ingestor.ingestFiling(cik = cikNumber!!, adsh = accessionNumber!!)
            } catch (e: Exception) {
                e.printStackTrace()
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
                                q4FactFinder.run(cik!!, year!!)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }
    }

}
