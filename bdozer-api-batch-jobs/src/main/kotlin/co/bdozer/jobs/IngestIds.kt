package co.bdozer.jobs

import co.bdozer.libraries.tenk.IdsIngestor
import kotlin.system.exitProcess

fun main() {
    IdsIngestor.ingestIds()
    exitProcess(0)
}