package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.dataclasses.Item

data class IncomeStatement(
    val items: List<Item>,
)