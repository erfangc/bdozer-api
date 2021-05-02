package com.bdozer.api.factbase.core.dataclasses

data class XbrlContext(
    val id: String,
    val entity: XbrlEntity,
    val period: XbrlPeriod
)
