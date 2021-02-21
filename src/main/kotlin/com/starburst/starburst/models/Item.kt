package com.starburst.starburst.models

data class Item(
    /**
     * [name] of this item, this is akin to an identifier
     */
    val name: String,

    /**
     * [description] for human reading
     */
    val description: String? = null,

    /**
     * [historicalValue] the latest actual value for this item
     */
    val historicalValue: Double = 0.0,

    /**
     * List of [Driver] that compose this item's value in the future
     * essentially components that aggregates into this [Item]
     */
    val drivers: List<Driver>? = emptyList(),

    /**
     * [expression] this is mutually exclusive with [Driver] in that
     * if [expression] is populated this item will take on the value specified
     * by [expression] instead of being aggregated up from [Driver]
     */
    val expression: String? = null,

    val segment: String? = null
)
