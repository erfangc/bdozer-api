package com.starburst.starburst.stockanalyzer.common

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.stockanalyzer.overrides.ModelOverrideService
import com.starburst.starburst.zacks.se.ZacksEstimatesService

class StockAnalyzerDataProvider(
    val zacksEstimatesService: ZacksEstimatesService,
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val modelOverrideService: ModelOverrideService,
    val filingEntity: FilingEntity,
)