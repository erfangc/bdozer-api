package com.starburst.starburst.spreadsheet

data class Address(
    val sheet: Int,
    val row: Int,
    val column: Int,
    val columnLetter: String,
)
