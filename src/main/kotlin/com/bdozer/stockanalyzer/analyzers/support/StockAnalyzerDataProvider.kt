package com.bdozer.stockanalyzer.analyzers.support

import com.bdozer.alphavantage.AlphaVantageService
import com.bdozer.edgar.factbase.FilingProvider
import com.bdozer.edgar.factbase.core.FactBase
import com.bdozer.filingentity.dataclasses.FilingEntity
import com.bdozer.zacks.se.ZacksEstimatesService

class StockAnalyzerDataProvider(
    val alphaVantageService: AlphaVantageService,
    val zacksEstimatesService: ZacksEstimatesService,
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val filingEntity: FilingEntity,
)