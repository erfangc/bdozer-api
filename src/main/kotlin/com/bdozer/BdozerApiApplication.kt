package com.bdozer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class BdozerApiApplication

fun main(args: Array<String>) {
    runApplication<BdozerApiApplication>(*args)
}

