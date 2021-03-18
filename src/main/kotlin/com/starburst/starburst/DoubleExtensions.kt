package com.starburst.starburst

import java.math.BigDecimal
import java.math.RoundingMode

object DoubleExtensions {
    fun Double?.fmtRound(precision: Int = 0): String {
        return if (this == null) {
            "0"
        } else {
            val scaledAndRounded = BigDecimal(this).setScale(precision, RoundingMode.HALF_EVEN)
            "$scaledAndRounded"
        }
    }

    fun Double?.fmtPct(precision: Int = 1): String {
        return if (this == null) {
            "0%"
        } else {
            val scaledAndRounded = BigDecimal(this * 100.0).setScale(precision, RoundingMode.HALF_EVEN)
            "$scaledAndRounded%"
        }
    }

    fun Double?.orZero() = this ?: 0.0
}