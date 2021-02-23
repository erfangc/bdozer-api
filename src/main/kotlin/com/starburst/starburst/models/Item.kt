package com.starburst.starburst.models

import com.starburst.starburst.models.translator.subtypes.dataclasses.PercentOfTotalAsset
import com.starburst.starburst.models.translator.subtypes.dataclasses.FixedCost
import com.starburst.starburst.models.translator.subtypes.dataclasses.SubscriptionRevenue
import com.starburst.starburst.models.translator.subtypes.dataclasses.PercentOfRevenue

data class Item(
    /**
     * [name] of this item, this is akin to an identifier
     */
    val name: String,

    /**
     * [description] for human reading
     */
    val description: String? = null,

    /**
     * [historicalValue] the latest actual value for this item
     */
    val historicalValue: Double = 0.0,

    val expression: String = "0.0",

    val segment: String? = null,

    val type: ItemType = ItemType.Custom,

    val subscriptionRevenue: SubscriptionRevenue? = null,

    val percentOfTotalAsset: PercentOfTotalAsset? = null,

    val percentOfRevenue: PercentOfRevenue? = null,

    val fixedCost: FixedCost? = null
)

