package com.bdozer.api.common.dataclasses.sec

data class XbrlEntity(
    val identifier: XbrlIdentifier,
    val segment: XbrlSegment? = null
)
