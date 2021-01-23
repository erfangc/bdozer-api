package com.starburst.starburst

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StarburstApplication

fun main(args: Array<String>) {
    runApplication<StarburstApplication>(*args)
}

