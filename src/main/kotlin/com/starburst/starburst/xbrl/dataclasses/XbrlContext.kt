package com.starburst.starburst.xbrl.dataclasses

data class XbrlContext(
    val id: String,
    val entity: XbrlEntity,
    val period: XbrlPeriod
)
