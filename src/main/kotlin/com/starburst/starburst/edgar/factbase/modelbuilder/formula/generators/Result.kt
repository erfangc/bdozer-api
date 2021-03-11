package com.starburst.starburst.edgar.factbase.modelbuilder.formula.generators

import com.starburst.starburst.models.Item

data class Result(
    val item: Item,
    val commentary: String? = null,
)