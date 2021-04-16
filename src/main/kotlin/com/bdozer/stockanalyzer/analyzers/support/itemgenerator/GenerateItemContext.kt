package com.bdozer.stockanalyzer.analyzers.support.itemgenerator

import com.bdozer.edgar.factbase.FilingProvider

data class GenerateItemContext(
    val filingProvider: FilingProvider,
)