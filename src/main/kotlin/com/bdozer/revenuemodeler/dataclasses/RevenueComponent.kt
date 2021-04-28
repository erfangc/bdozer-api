package com.bdozer.revenuemodeler.dataclasses

data class RevenueComponent(
    val label: String,
    val description: String,
    val values: List<Value>,
) {
    fun latest(): Value {
        return values.maxByOrNull { it.year } ?: error("...")
    }
}