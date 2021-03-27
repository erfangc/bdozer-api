package com.starburst.starburst

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class StarburstApplication

fun main(args: Array<String>) {
    runApplication<StarburstApplication>(*args)
}

