package com.starburst.starburst.misc

import java.time.Instant
import java.util.*

data class RegisteredEmail(
    val _id: String = UUID.randomUUID().toString(),
    val timestamp: String = Instant.now().toString(),
    val email: String
)
