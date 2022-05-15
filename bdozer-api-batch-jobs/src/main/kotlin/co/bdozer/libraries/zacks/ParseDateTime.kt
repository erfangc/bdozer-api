package co.bdozer.libraries.zacks

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH'h'mm'm'ss")
fun String?.localDateTime() = if (this == null)
    null
else
    LocalDateTime.parse(this, pattern)