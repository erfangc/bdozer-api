package com.starburst.starburst.edgar.factbase.dataclasses

import com.starburst.starburst.edgar.factbase.dataclasses.Fact

data class FactComponentsResponse(
    val facts: List<Fact>,
    val latestAnnualFacts: List<Fact>,
    val latestQuarterFacts: List<Fact>
)