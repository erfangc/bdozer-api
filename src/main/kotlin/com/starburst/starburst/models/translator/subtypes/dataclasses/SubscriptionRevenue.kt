package com.starburst.starburst.models.translator.subtypes.dataclasses

data class SubscriptionRevenue(
    val totalSubscriptionAtTerminalYear: Int,
    val initialSubscriptions: Int,
    val averageRevenuePerSubscription: Double
)
