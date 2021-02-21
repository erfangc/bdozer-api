package com.starburst.starburst.models

import com.starburst.starburst.models.translator.subtypes.CustomDriver
import com.starburst.starburst.models.translator.subtypes.FixedCost
import com.starburst.starburst.models.translator.subtypes.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.VariableCost

data class Driver(
    val name: String,
    val type: DriverType,
    val saaSRevenue: SaaSRevenue? = null,
    val variableCost: VariableCost? = null,
    val fixedCost: FixedCost? = null,
    val customDriver: CustomDriver? = null,
    val historicalValue: Double = 0.0
)
