package co.bdozer.libraries.tenk

import CompanyText
import co.bdozer.libraries.tenk.models.Submission
import co.bdozer.libraries.tenk.sectionparser.TenKSectionExtractor
import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database.runSql
import co.bdozer.libraries.utils.HashGenerator.hash
import co.bdozer.libraries.utils.HtmlToPlainText.plainText
import com.fasterxml.jackson.module.kotlin.readValue
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object TenKProcessor {

    private val log = LoggerFactory.getLogger(TenKProcessor::class.java)
    private val objectMapper = Beans.objectMapper()
    private val tenKSectionExtractor = TenKSectionExtractor()

    fun buildCompanyText(ticker: String): List<CompanyText> {
        val cik = toCik(ticker)

        /*
        Find the latest submission and print out the raw text
         */
        val submission = submission(cik)

        val form = "10-K"
        val idx = submission.filings?.recent?.form?.indexOfFirst { it == form }
            ?: error("cannot find form $form for ticker $ticker")
        val recent = submission.filings.recent
        val ash = recent.accessionNumber?.get(idx) ?: error("...")
        val reportDate = recent.reportDate?.get(idx) ?: error("...")

        val primaryDocument = recent.primaryDocument?.get(idx) ?: error("...")
        val url = "https://www.sec.gov/Archives/edgar/data/$cik/${ash.replace("-", "")}/$primaryDocument"
        log.info("Parsing form=$form cik=$cik ash=$ash primaryDocument=$primaryDocument url=${url} ")

        val doc = Jsoup.connect(url).get()
        val sections = tenKSectionExtractor.extractSections(doc)
        val elements = sections?.business?.elements
        val body = Element("body")
        elements?.forEach { body.appendChild(it) }

        val textBody = plainText(body)

        val paragraphs = textBody
            .split("\n+".toRegex())
            .filter {
                val trimmed = it.trim()
                trimmed.isNotBlank() && trimmed.length >50 
            }
            .mapIndexed { index, paragraph ->
                CompanyText(
                    id = hash(ticker, url, index.toString()),
                    ticker = ticker,
                    text = paragraph.trim(),
                    url = url,
                    metaData = mapOf(
                        "cik" to cik,
                        "ash" to ash,
                        "index" to index.toString(),
                        "reportDate" to reportDate,
                    ),
                    source = "10-K"
                )
            }

        return paragraphs
    }

    private fun submission(cik: String): Submission {
        val inputStream = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().GET().uri(URI.create("https://data.sec.gov/submissions/CIK${cik}.json")).build(),
            HttpResponse.BodyHandlers.ofInputStream(),
        ).body()
        return objectMapper.readValue(inputStream)
    }

    private fun toCik(ticker: String): String {
        val row = runSql(
            sql = """
            select cik from ids where ticker = '$ticker'
        """.trimIndent()
        ).first()
        val cik = row["cik"]?.toString() ?: error("cannot find cik for ticker $ticker")
        log.info("Resolved ticker $ticker to cik $cik")
        return cik
    }

}

