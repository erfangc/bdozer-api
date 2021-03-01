package com.starburst.starburst.models

import com.starburst.starburst.models.translator.subtypes.dataclasses.*

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
    val historicalValue: Double = 0.0,

    val expression: String = "0.0",

    val segment: String? = null,

    val subscriptionRevenue: SubscriptionRevenue? = null,

    val percentOfTotalAsset: PercentOfTotalAsset? = null,

    val percentOfRevenue: PercentOfRevenue? = null,

    val unitSalesRevenue: UnitSalesRevenue? = null,

    val fixedCost: FixedCost? = null,

    val stockBasedCompensation: Boolean = false,

    val nonCashExpense: Boolean = false,

    /**
     * [subtotal]
     */
    val subtotal: Boolean = false
)

