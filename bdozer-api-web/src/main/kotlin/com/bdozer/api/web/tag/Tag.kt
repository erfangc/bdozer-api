package com.bdozer.api.web.tag

import java.time.Instant

data class Tag(
    val _id: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
)
