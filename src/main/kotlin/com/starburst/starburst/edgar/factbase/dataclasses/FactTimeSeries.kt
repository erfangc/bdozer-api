package com.starburst.starburst.edgar.factbase.dataclasses

data class FactTimeSeries(
    val fyFacts: List<Fact>,
    val quarterlyFacts: List<Fact>,
    val ltmFacts: List<Fact>,
)