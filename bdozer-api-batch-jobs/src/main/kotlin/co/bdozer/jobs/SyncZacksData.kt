package co.bdozer.jobs

import co.bdozer.libraries.zacks.ZacksProcessorDriver
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")
fun main() {
    log.info("Synchronizing Zacks data")
    ZacksProcessorDriver.processAllDatasets()
    log.info("Finished synchronizing Zacks data")
    exitProcess(0)
}