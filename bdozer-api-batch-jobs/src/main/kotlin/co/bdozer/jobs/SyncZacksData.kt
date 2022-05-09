package co.bdozer.jobs

import co.bdozer.libraries.zacks.ZacksTableSyncer
import kotlin.system.exitProcess

fun main() {
    ZacksTableSyncer.syncTables()
    exitProcess(0)
}