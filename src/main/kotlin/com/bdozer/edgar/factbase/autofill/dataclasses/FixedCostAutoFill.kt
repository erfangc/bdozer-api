package com.bdozer.edgar.factbase.autofill.dataclasses

import com.bdozer.models.dataclasses.FixedCost

/**
 * [FactAutoFillService] provides methods that automatically determines
 * properties on [com.bdozer.models.dataclasses.Item] such as
 * [com.bdozer.models.dataclasses.PercentOfRevenue] based on facts in FactBase
 */

data class FixedCostAutoFill(
    val label: String,
    val fixedCost: FixedCost
)