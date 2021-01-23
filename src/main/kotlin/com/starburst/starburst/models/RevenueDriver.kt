package com.starburst.starburst.models

import com.starburst.starburst.models.enums.RevenueDriverType

data class RevenueDriver(
    val name: String? = null,
    val type: RevenueDriverType = RevenueDriverType.Default,
    val subscriptionDriver: SubscriptionDriver? = null
)

