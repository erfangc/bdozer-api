package com.starburst.starburst.modelbuilder.templates

import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.spreadsheet.Cell

data class ModelResult(
    val cells: List<Cell>,
    /*
    value breakdown
     */
    val zeroGrowthPrice: Double,
    val impliedPriceFromGrowth: Double,
    val currentPrice: Double,

    val model: Model,

    /*
    revenue waterfall
     */
    val revenue: Item,
    val categorizedExpenses: List<Item>,
    val profit: Item,
    val shareOutstanding: Item,
    val profitPerShare: Item,

    /*
    target price
     */
    val targetPrice: Double
)