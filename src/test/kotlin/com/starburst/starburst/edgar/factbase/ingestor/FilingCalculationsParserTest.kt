package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.factbase.DBXFilingProvider.filingProvider
import com.starburst.starburst.edgar.factbase.ingestor.support.FilingCalculationsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FilingCalculationsParserTest {

    @Test
    fun parseStatements() {
        val parser = FilingCalculationsParser(filingProvider = filingProvider())
        val calculations = parser.parseCalculations()
        assertEquals(18, calculations.incomeStatement.size)
        assertEquals(42, calculations.balanceSheet.size)
        assertEquals(49, calculations.cashFlowStatement.size)
    }
}