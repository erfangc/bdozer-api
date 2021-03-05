package com.starburst.starburst.edgar.explorer.dataclasses

data class EdgarEntitySource(
    val entity: String,
    val entity_words: String,
    val tickers: String? = null,
    val rank: Long? = null,
)