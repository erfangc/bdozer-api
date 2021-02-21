package com.starburst.starburst.models.translator.subtypes.dataclasses

data class SaaSRevenue(
    val totalSubscriptionAtTerminalYear: Int,
    val initialSubscriptions: Int,
    val averageRevenuePerSubscription: Double
)
