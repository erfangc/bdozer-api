package com.starburst.starburst.models.dataclasses

data class Discrete(
    /**
     * A map of period to formula that should be applied
     * to that period
     */
    val formulas: Map<Int, String>
)