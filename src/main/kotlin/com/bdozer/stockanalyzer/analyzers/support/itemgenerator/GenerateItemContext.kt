package com.bdozer.stockanalyzer.analyzers.support.itemgenerator

import com.bdozer.edgar.provider.FilingProvider
import com.bdozer.edgar.factbase.dataclasses.Dimension

data class GenerateItemContext(
    val filingProvider: FilingProvider,
    val dimensions: List<Dimension>,
)