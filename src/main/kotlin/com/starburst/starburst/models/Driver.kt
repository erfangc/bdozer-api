package com.starburst.starburst.models

import com.starburst.starburst.models.translator.subtypes.dataclasses.CustomDriver
import com.starburst.starburst.models.translator.subtypes.dataclasses.FixedCost
import com.starburst.starburst.models.translator.subtypes.dataclasses.SaaSRevenue
import com.starburst.starburst.models.translator.subtypes.dataclasses.VariableCost

data class Driver(
    val name: String,
    val type: DriverType,
    val saaSRevenue: SaaSRevenue? = null,
    val variableCost: VariableCost? = null,
    val fixedCost: FixedCost? = null,
    val customDriver: CustomDriver? = null,
    val historicalValue: Double = 0.0
)
