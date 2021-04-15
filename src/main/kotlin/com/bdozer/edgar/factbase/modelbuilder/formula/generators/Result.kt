package com.bdozer.edgar.factbase.modelbuilder.formula.generators

import com.bdozer.models.dataclasses.Item

data class Result(
    val item: Item,
    val commentary: String? = null,
)