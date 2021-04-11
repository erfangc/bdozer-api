package com.starburst.starburst.models.dataclasses

import com.starburst.starburst.models.translator.subtypes.CompoundedGrowth

data class Item(
    /**
     * [name] of this item, this is akin to an identifier
     */
    val name: String,

    /**
     * [description] for human reading
     */
    val description: String? = null,

    val type: ItemType = ItemType.Custom,

    /**
     * [historicalValue] the latest actual value for this item
     */
    val historicalValue: HistoricalValue? = null,

    val formula: String = "0.0",

    val subscriptionRevenue: SubscriptionRevenue? = null,

    val unitSalesRevenue: UnitSalesRevenue? = null,

    val discrete: Discrete? = null,

    val percentOfTotalAsset: PercentOfTotalAsset? = null,

    val percentOfRevenue: PercentOfRevenue? = null,
    val compoundedGrowth: CompoundedGrowth? = null,
    val fixedCost: FixedCost? = null,

    val stockBasedCompensation: Boolean? = null,
    val nonCashExpense: Boolean? = null,
    val commentaries: Commentary? = null,
    val subtotal: Boolean = false,

    )

