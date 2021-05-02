package com.bdozer.api.common.dataclasses.sec

data class XbrlContext(
    val id: String,
    val entity: XbrlEntity,
    val period: XbrlPeriod
)
