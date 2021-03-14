package com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import java.math.BigDecimal
import java.math.RoundingMode

object CommentaryExtensions {
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

    fun FormulaGenerator.withCommentary(result: Result): Item {
        val clazz = this::class.java.simpleName
        return result.item.copy(
            commentaries = Commentary(commentary = result.commentary, generatorClass = clazz)
        )
    }
}