package com.bdozer.api.models.dataclasses.spreadsheet

data class Address(
    val sheet: Int,
    val sheetName: String,
    val row: Int,
    val column: Int,
    val columnLetter: String,
) {
    companion object {
        val noop = Address(
            sheet = 0,
            sheetName = "",
            row = 0,
            column = 0,
            columnLetter = "",
        )
    }
}
