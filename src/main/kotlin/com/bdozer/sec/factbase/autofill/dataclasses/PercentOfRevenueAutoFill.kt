package com.bdozer.sec.factbase.autofill.dataclasses

import com.bdozer.models.dataclasses.PercentOfRevenue

/**
 * [FactAutoFillService] provides methods that automatically determines
 * properties on [com.bdozer.models.dataclasses.Item] such as
 * [com.bdozer.models.dataclasses.PercentOfRevenue] based on facts in FactBase
 */

data class PercentOfRevenueAutoFill(
    val label: String,
    val percentOfRevenue: PercentOfRevenue
)