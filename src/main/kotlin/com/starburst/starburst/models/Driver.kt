package com.starburst.starburst.models

import com.starburst.starburst.computers.drivers.FixedCost
import com.starburst.starburst.computers.drivers.SaaSRevenue
import com.starburst.starburst.computers.drivers.VariableCost

data class Driver(
    val name: String,
    val type: DriverType,
    val saaSRevenue: SaaSRevenue? = null,
    val variableCost: VariableCost? = null,
    val fixedCost: FixedCost? = null
)

enum class DriverType {
    SaaSRevenue,
    VariableCost,
    FixedCost
}
