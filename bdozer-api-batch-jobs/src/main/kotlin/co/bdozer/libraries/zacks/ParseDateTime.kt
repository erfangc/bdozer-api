package co.bdozer.libraries.zacks

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun parseDateTime(input: String)=
    LocalDateTime.parse("2022-05-15T15h47m59", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH'h'mm'm'ss"))