package com.bdozer.revenuemodeler.dataclasses

data class RevenueDriver(
    val component1: RevenueComponent,
    val component2: RevenueComponent,
    val operator: Operator,
)