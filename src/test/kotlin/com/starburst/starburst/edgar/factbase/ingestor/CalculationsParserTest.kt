package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.factbase.DBXFilingProvider.filingProvider
import com.starburst.starburst.edgar.factbase.ingestor.support.CalculationsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CalculationsParserTest {

    @Test
    fun parseStatements() {
        val parser = CalculationsParser(filingProvider = filingProvider())
        val calculations = parser.parseCalculations()
        assertEquals(18, calculations.incomeStatement.size)
        assertEquals(42, calculations.balanceSheet.size)
        assertEquals(49, calculations.cashFlowStatement.size)
    }
}