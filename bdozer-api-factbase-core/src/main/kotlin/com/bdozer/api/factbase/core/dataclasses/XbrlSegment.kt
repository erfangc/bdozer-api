package com.bdozer.api.factbase.core.dataclasses

import java.util.Collections.emptyList

data class XbrlSegment(
    val explicitMembers: List<XbrlExplicitMember> = emptyList()
)
