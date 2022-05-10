package co.bdozer.jobs

import co.bdozer.libraries.indexer.Indexer.index
import co.bdozer.libraries.master.CompanyMasterBuilder
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")
fun main() {

    val lines = File("bdozer-api-batch-jobs/russell-1000-constituents.txt")
        .bufferedReader()
        .readLines()
    var remaining = lines.size
    var total = 0
    var success = 0
    val failures = arrayListOf<Exception>()

    lines.forEach { line ->
        val ticker = line.trim()
        try {
            log.info("Processing ticker $ticker")
            val companyMasterRecord = CompanyMasterBuilder.buildCompanyRecord(ticker)
            index(ticker, companyMasterRecord)
            log.info("Finished processing $ticker")
            success++
        } catch (e: Exception) {
            log.error("Failed to process $ticker error={}", e.message)
            failures.add(e)
        } finally {
            total++
            remaining--
            log.info("Status remaining={} total={}, success={}, failures={}", remaining, total, success, failures.size)
        }
    }

    log.info("Total of {} failures, see below for breakdown", failures.size)
    failures.groupBy { it.javaClass }.forEach { (exceptionClass, exceptions) ->
        log.info("exceptionClass={}, count={}", exceptionClass.name, exceptions.size)
    }
    exitProcess(0)
}