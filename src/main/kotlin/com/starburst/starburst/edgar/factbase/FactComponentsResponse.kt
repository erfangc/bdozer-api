package com.starburst.starburst.edgar.factbase

data class FactComponentsResponse(
    val facts: List<Fact>,
    val latestAnnualFacts: List<Fact>,
    val latestQuarterFacts: List<Fact>
)