package com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.FormulaGenerator
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators.Result
import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import java.math.BigDecimal
import java.math.RoundingMode

object CommentaryExtensions {
    fun FormulaGenerator.withCommentary(result: Result): Item {
        val clazz = this::class.java.simpleName
        return result.item.copy(
            commentaries = Commentary(commentary = result.commentary, generatorClass = clazz)
        )
    }
}