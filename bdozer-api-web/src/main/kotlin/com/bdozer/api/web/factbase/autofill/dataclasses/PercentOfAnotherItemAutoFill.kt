package com.bdozer.api.web.factbase.autofill.dataclasses

import bdozer.api.common.model.PercentOfAnotherItem

/**
 * [FactAutoFillService] provides methods that automatically determines
 * properties on [com.bdozer.models.dataclasses.Item] such as
 * [com.bdozer.models.dataclasses.PercentOfRevenue] based on facts in FactBase
 */
data class PercentOfAnotherItemAutoFill(
    val label: String,
    val percentOfAnotherItem: PercentOfAnotherItem
)