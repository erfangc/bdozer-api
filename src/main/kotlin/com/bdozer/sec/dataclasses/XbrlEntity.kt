package com.bdozer.sec.dataclasses

data class XbrlEntity(
    val identifier: XbrlIdentifier,
    val segment: XbrlSegment? = null
)