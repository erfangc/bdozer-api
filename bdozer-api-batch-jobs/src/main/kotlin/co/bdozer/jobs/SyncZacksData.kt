package co.bdozer.jobs

import co.bdozer.libraries.zacks.ZacksTableSyncer
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")
fun main() {
    log.info("Synchronizing Zacks data")
    ZacksTableSyncer.syncTables()
    log.info("Finished synchronizing Zacks data")
    exitProcess(0)
}