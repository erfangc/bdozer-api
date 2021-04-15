package com.bdozer.models.dataclasses

data class SubscriptionRevenue(
    val totalSubscriptionAtTerminalYear: Double = 0.0,
    val initialSubscriptions: Double = 0.0,
    val averageRevenuePerSubscription: Double = 0.0
)
