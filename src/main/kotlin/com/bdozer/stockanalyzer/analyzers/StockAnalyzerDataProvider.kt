package com.bdozer.stockanalyzer.analyzers

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.edgar.FilingProvider
import com.bdozer.edgar.factbase.FactBase
import com.bdozer.filingentity.dataclasses.FilingEntity
import com.bdozer.zacks.se.ZacksEstimatesService

class StockAnalyzerDataProvider(
    val alphaVantageService: AlphaVantageService,
    val zacksEstimatesService: ZacksEstimatesService,
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val filingEntity: FilingEntity,
)