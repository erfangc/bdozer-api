package com.bdozer.api.factbase.core.dataclasses

data class XbrlEntity(
    val identifier: XbrlIdentifier,
    val segment: XbrlSegment? = null
)
