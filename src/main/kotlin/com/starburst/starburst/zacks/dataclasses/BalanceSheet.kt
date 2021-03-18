package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.dataclasses.Item

data class BalanceSheet(
    val items: List<Item>,
)