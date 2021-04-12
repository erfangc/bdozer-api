package com.starburst.starburst.edgar.factbase.autofill.dataclasses

import com.starburst.starburst.models.dataclasses.FixedCost

/**
 * [FactAutoFillService] provides methods that automatically determines
 * properties on [com.starburst.starburst.models.dataclasses.Item] such as
 * [com.starburst.starburst.models.dataclasses.PercentOfRevenue] based on facts in FactBase
 */

data class FixedCostAutoFill(
    val label: String,
    val fixedCost: FixedCost
)