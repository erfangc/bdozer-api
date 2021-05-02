package com.bdozer.api.common.dataclasses.sec

import java.util.Collections.emptyList

data class XbrlSegment(
    val explicitMembers: List<XbrlExplicitMember> = emptyList()
)
