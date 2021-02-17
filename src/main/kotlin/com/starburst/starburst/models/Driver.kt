package com.starburst.starburst.models

import com.starburst.starburst.computers.expression.resolvers.Custom
import com.starburst.starburst.computers.expression.resolvers.FixedCost
import com.starburst.starburst.computers.expression.resolvers.SaaSRevenue
import com.starburst.starburst.computers.expression.resolvers.VariableCost

data class Driver(
    val name: String,
    val type: DriverType,
    val saaSRevenue: SaaSRevenue? = null,
    val variableCost: VariableCost? = null,
    val fixedCost: FixedCost? = null,
    val custom: Custom? = null
)
