package com.starburst.starburst.edgar.factbase.modelbuilder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DependencyFlattenerTest {

    @Test
    fun go() {
        val flattener = DependencyFlattener(
            input = mapOf(
                "Revenue" to listOf(),
                "Segment A COGS" to listOf(),
                "Segment B COGS" to listOf(),
                "COGS" to listOf("Segment A COGS", "Segment B COGS"),
                "Gross Profit" to listOf("Revenue", "COGS"),
                "Selling" to listOf(),
                "R&D" to listOf(),
                "Operating Expenses" to listOf("Selling", "R&D"),
                "Operating Income" to listOf("Operating Expenses", "Gross Profit"),
                "Taxes" to listOf(),
                "Net Income" to listOf("Operating Income", "Taxes"),
            )
        )
        val output = flattener.flatten()
        assertEquals(output["Net Income"]?.size, 6)
    }
}