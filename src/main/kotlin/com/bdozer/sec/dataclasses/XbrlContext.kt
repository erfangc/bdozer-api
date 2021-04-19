package com.bdozer.sec.dataclasses

data class XbrlContext(
    val id: String,
    val entity: XbrlEntity,
    val period: XbrlPeriod
)
