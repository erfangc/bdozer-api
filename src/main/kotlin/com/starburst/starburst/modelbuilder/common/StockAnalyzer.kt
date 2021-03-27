package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.modelbuilder.dataclasses.StockAnalysis

interface StockAnalyzer {
    fun analyze(): StockAnalysis
}