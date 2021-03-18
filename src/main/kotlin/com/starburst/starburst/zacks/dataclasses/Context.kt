package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.fa.ZacksFundamentalA
import com.starburst.starburst.zacks.se.ZacksSalesEstimates

data class Context(
    val ticker: String,
    val model: Model,
    val zacksFundamentalA: List<ZacksFundamentalA>,
    val zacksSalesEstimates: List<ZacksSalesEstimates>,
) {
    fun latestAnnual(): ZacksFundamentalA {
        return zacksFundamentalA
            .filter { it.per_type == "A" }
            .maxByOrNull { it.per_end_date!! }
            ?: error("unable to find an annual fundamental for $ticker, found total fundamentals")
    }
}