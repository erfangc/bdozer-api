package com.starburst.starburst.models

data class SubscriptionDriver(
    val name: String,
    val perSubscriptionRevenue: Double?,
    val initialUnits: Int? = null,
    val initialRevenue: Double? = null,
    val tam: Double? = null,
    val yearsToReachTam: Int? = null,
)
