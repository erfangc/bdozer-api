package co.bdozer.libraries.tenk

import co.bdozer.libraries.tenk.models.CompanyTicker
import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

object IdsIngestor {

    private const val url = "https://www.sec.gov/files/company_tickers.json"
    private val objectMapper = Beans.objectMapper()
    private val httpClient = Beans.httpClient()
    private val connection = Database.connection
    private val log = LoggerFactory.getLogger(IdsIngestor::class.java)

    fun ingestIds() {
        val httpResponse = httpClient
            .send(
                HttpRequest
                    .newBuilder(URI.create(url))
                    .GET()
                    .build(),
                BodyHandlers.ofInputStream()
            )
        val inputStream = httpResponse.body()
        val tickers = objectMapper.readValue<Map<String, CompanyTicker>>(inputStream)
        connection.autoCommit = false
        truncateTable()

        var total = 0
        tickers
            .entries
            .distinctBy { it.value.cik_str }
            .chunked(100)
            .forEach { chunk ->
                val stmt = connection.prepareStatement("insert into ids (cik, ticker, company_name) values (?, ?, ?)")
                chunk.forEach {
                    val companyTicker = it.value
                    val paddedCik = companyTicker.cik_str.padStart(length = 10, padChar = '0')
                    stmt.setString(1, paddedCik)
                    stmt.setString(2, companyTicker.ticker)
                    stmt.setString(3, companyTicker.title)
                    stmt.addBatch()
                }
                stmt.executeBatch()
                connection.commit()
                total += chunk.size
                log.info("Inserted ${chunk.size} entries, total=$total")
            }
        connection.autoCommit = true
    }

    private fun truncateTable() {
        // truncate the existing table
        val stmt = connection.createStatement()
        stmt.execute("truncate ids")
        log.info("Truncating table 'ids'")
    }

}