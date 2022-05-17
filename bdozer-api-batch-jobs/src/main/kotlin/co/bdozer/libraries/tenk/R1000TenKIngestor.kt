package co.bdozer.libraries.tenk

import co.bdozer.libraries.indexer.Indexer
import co.bdozer.libraries.tenk.TenKProcessor.buildCompanyText
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import kotlin.system.exitProcess

/**
 * This program takes a list of S&P companies
 * and for each one ... queries its CIK and then ingest the latest 10-K filing
 */
object R1000TenKIngestor {

    private val log = LoggerFactory.getLogger(R1000TenKIngestor::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        ingestR1000ConstituentTenKs()
        exitProcess(0)
    }
    fun ingestR1000ConstituentTenKs() {

        val filename = "bdozer-api-batch-jobs/russell-1000-constituents.txt"
        log.info("Starting ingesting Russell 1000 constituent 10-Ks using embedded file $filename")
        val tickers = FileInputStream(filename)
            .bufferedReader()
            .readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        log.info("Found {} lines in {}", tickers.size, filename)

        var remaining = tickers.size
        var total = 0
        val failures = arrayListOf<Exception>()
        var success = 0

        for (ticker in tickers) {
            log.info("Processing ticker {}", ticker)
            try {
                val companyTexts = buildCompanyText(ticker)
                val start = System.currentTimeMillis()
                Indexer.bulkIndex("companyText", companyTexts.map { it.id to it })
                val stop = System.currentTimeMillis()
                log.info("Successfully index {} paragraphs for ticker {}, took={}ms", companyTexts.size, ticker, stop - start)
                success++
            } catch (e: Exception) {
                log.error("Exception occurred while processing ticker={}, error={}", ticker, e.message)
                e.printStackTrace()
                failures.add(e)
            } finally {
                remaining--
                total++
                log.info(
                    "Status total={} remaining={} success={} failures={}",
                    total,
                    remaining,
                    success,
                    failures.size,
                )
            }
        }
        failures
            .groupBy {
                it.message
            }
            .forEach { (msg, exceptions) ->
                log.info("Failure message='{}' count={}", msg, exceptions.size)
            }
        exitProcess(0)
    }

}