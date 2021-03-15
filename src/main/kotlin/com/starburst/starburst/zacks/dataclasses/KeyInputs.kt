package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.dataclasses.Discrete

data class KeyInputs(
    val _id: String,
    val keyInputs: List<KeyInput>,
    val discrete: Discrete? = null,
)