package com.starburst.starburst.edgar.factbase.dataclasses

import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Calculation

data class FindFactComponentsResponse(
    val componentFacts: List<Fact>,
    val latestAnnual: List<Fact>,
    val latestQuarterly: List<Fact>,
    val calculations: List<Calculation>,
)