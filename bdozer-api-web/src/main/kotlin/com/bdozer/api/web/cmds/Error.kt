package com.bdozer.api.web.cmds

data class Error(
    val cik: String,
    val ticker: String,
    val message: String,
)