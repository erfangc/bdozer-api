package com.starburst.starburst.edgar.factbase.dataclasses

data class FindFactComponentsResponse(
    val componentFacts: List<Fact>,
    val latestAnnual: List<Fact>,
    val latestQuarterly: List<Fact>,
    val calculations: List<Calculation>,
)