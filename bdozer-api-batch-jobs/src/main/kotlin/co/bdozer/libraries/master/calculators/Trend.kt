package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.master.models.Trend
import co.bdozer.libraries.zacks.models.FC

fun trend(fcs: List<FC>, extractor: (FC) -> Double?): Trend {
    val fcs = fcs.take(6).sortedByDescending { it.qtr_nbr }
    val pctChanges = fcs
        .windowed(2)
        .map {
            val curr = extractor.invoke(it.first()) ?: 0.0
            val prev = extractor.invoke(it.last())
            if (prev == null) {
                0.0
            } else {
                // if prev value and curr value have different signs the % change won't be meaningful
                // in that case we return nothing
                if (curr * prev < 0) {
                    0.0
                } else if (curr < 0 && prev < 0) {
                    // if prev and current value are both negative, the percentage change will
                    // also not be meaningful
                    0.0
                } else {
                    (curr - prev) / prev
                }
            }
        }

    val changes = fcs
        .windowed(2)
        .map {
            val curr = extractor.invoke(it.first()) ?: 0.0
            val prev = extractor.invoke(it.last()) ?: 0.0
            curr - prev
        }

    // we say growth rate increasing if the most recent period is increasing
    // and there are more increases than decreases in the remaining period
    val isIncreasing = if (changes.isNotEmpty()) {
        if (changes[0] > 0) {
            // the latest change is positive
            val takes = changes.take(4)
            val numPositiveChanges = takes
                .filter { it > 0 }
                .size
            numPositiveChanges > (takes.size / 2)
        } else {
            false
        }
    } else {
        false
    }

    // we say growth rate is erratic if there are roughly equal number of increases and decreases
    val take = changes.take(5)
    val isErratic = (take.filter { it < 0 }.size - take.filter { it > 0 }.size) < 2

    return Trend(
        isErratic = isErratic,
        isIncreasing = isIncreasing,

        thisQuarter = if (changes.isNotEmpty()) changes[0] else null,
        oneQuarterAgo = if (changes.size > 1) changes[1] else null,
        twoQuartersAgo = if (changes.size > 2) changes[2] else null,
        threeQuartersAgo = if (changes.size > 3) changes[3] else null,
        fourQuartersAgo = if (changes.size > 4) changes[4] else null,

        thisQuarterPctChange = if (pctChanges.isNotEmpty()) pctChanges[0] else null,
        oneQuarterAgoPctChange = if (pctChanges.size > 1) pctChanges[1] else null,
        twoQuartersAgoPctChange = if (pctChanges.size > 2) pctChanges[2] else null,
        threeQuartersAgoPctChange = if (pctChanges.size > 3) pctChanges[3] else null,
        fourQuartersAgoPctChange = if (pctChanges.size > 4) pctChanges[4] else null,
    )
}