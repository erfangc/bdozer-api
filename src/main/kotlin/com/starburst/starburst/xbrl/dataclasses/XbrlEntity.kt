package com.starburst.starburst.xbrl.dataclasses

data class XbrlEntity(
    val identifier: XbrlIdentifier,
    val segment: XbrlSegment? = null
)
