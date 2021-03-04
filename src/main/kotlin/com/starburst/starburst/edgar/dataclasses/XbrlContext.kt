package com.starburst.starburst.edgar.dataclasses

data class XbrlContext(
    val id: String,
    val entity: XbrlEntity,
    val period: XbrlPeriod
)
