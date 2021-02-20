package com.starburst.starburst.models

import com.starburst.starburst.models.translator.resolvers.CustomDriver
import com.starburst.starburst.models.translator.resolvers.FixedCost
import com.starburst.starburst.models.translator.resolvers.SaaSRevenue
import com.starburst.starburst.models.translator.resolvers.VariableCost

data class Driver(
    val name: String,
    val type: DriverType,
    val saaSRevenue: SaaSRevenue? = null,
    val variableCost: VariableCost? = null,
    val fixedCost: FixedCost? = null,
    val customDriver: CustomDriver? = null
)
