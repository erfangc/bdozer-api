package com.starburst.starburst.zacks.dataclasses

data class TalkingPoint(
    val data: Double? = null,
    val projections: List<Projection> = emptyList(),
    val commentary: String? = null,
    val forwardCommentary: String? = null,
)