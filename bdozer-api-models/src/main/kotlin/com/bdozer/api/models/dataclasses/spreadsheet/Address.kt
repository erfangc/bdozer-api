package com.bdozer.api.models.dataclasses.spreadsheet

data class Address(
    val sheet: Int,
    val sheetName: String,
    val row: Int,
    val column: Int,
    val columnLetter: String,
)
