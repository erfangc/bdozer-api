package com.bdozer.api.web.spreadsheet

data class Address(
    val sheet: Int,
    val sheetName: String,
    val row: Int,
    val column: Int,
    val columnLetter: String,
)
