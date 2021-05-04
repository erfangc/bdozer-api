package com.bdozer.api.ml.worker.cmds

data class Error(
    val cik: String,
    val ticker: String,
    val message: String,
)