package com.starburst.starburst.stockanalyzer.common

import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis

interface StockAnalyzer {
    fun analyze(): StockAnalysis
}