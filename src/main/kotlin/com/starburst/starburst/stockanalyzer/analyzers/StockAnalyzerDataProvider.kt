package com.starburst.starburst.stockanalyzer.analyzers

import com.starburst.starburst.alphavantage.AlphaVantageService
import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.zacks.se.ZacksEstimatesService

class StockAnalyzerDataProvider(
    val alphaVantageService: AlphaVantageService,
    val zacksEstimatesService: ZacksEstimatesService,
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val filingEntity: FilingEntity,
)