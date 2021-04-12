package com.starburst.starburst.edgar.factbase.autofill.dataclasses

import com.starburst.starburst.models.dataclasses.PercentOfRevenue

/**
 * [FactAutoFillService] provides methods that automatically determines
 * properties on [com.starburst.starburst.models.dataclasses.Item] such as
 * [com.starburst.starburst.models.dataclasses.PercentOfRevenue] based on facts in FactBase
 */

data class PercentOfRevenueAutoFill(
    val label: String,
    val percentOfRevenue: PercentOfRevenue
)