package com.bdozer.api.web

import de.dentrassi.crypto.pem.PemKeyStoreProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import java.security.Security


@EnableCaching
@EnableScheduling
@SpringBootApplication
class BdozerApiApplication

fun main(args: Array<String>) {
    Security.addProvider(PemKeyStoreProvider())
    runApplication<BdozerApiApplication>(*args)
}
