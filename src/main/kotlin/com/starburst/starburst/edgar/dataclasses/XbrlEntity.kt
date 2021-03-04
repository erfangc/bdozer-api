package com.starburst.starburst.edgar.dataclasses

data class XbrlEntity(
    val identifier: XbrlIdentifier,
    val segment: XbrlSegment? = null
)
