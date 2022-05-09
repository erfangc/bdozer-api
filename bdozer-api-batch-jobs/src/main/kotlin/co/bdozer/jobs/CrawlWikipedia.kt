package co.bdozer.jobs

import co.bdozer.libraries.indexer.Indexer
import co.bdozer.libraries.wikipedia.WikipediaCrawler
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")

fun main() {
    val allTickers = WikipediaCrawler.getAllTickers()
    var remaining = allTickers.size
    var total = 0
    var blank = 0
    var success = 0
    val failures = arrayListOf<Exception>()

    for (ticker in allTickers) {
        log.info("Processing $ticker")
        try {
            val companyText = WikipediaCrawler.getCompanyText(ticker)
            if (companyText != null) {
                Indexer.index(companyText.id, companyText)
                success++
            } else {
                blank++
            }
        } catch (e: Exception) {
            log.error("Unable to process ticker={} message={} exception={}", ticker, e.message, e.javaClass.simpleName)
            failures.add(e)
        } finally {
            total++
            remaining--
            log.info(
                "Status total={} remaining={} success={} failures={} blank={}",
                total,
                remaining,
                success,
                failures.size,
                blank
            )
        }
    }
    failures
        .groupBy { it.message }
        .forEach { (msg, exps) ->
            log.info("Failure summary message='{}', count={}", msg, exps.size)
        }
    exitProcess(0)
}