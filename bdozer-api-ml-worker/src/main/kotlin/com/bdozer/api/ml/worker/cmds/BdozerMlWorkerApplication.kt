package com.bdozer.api.ml.worker.cmds

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BdozerMlWorkerApplication

fun main(args: Array<String>) {
    runApplication<BdozerMlWorkerApplication>(*args)
}