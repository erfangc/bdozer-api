package com.bdozer.edgar.factbase.ingestor

import com.bdozer.edgar.factbase.FilingProviderProvider.dbx202010k
import com.bdozer.edgar.factbase.FilingArcsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FilingArcsParserTest {
    @Test
    fun parseStatements() {
        val parser = FilingArcsParser(filingProvider = dbx202010k())
        val calculations = parser.parseFilingArcs()
        assertEquals(18, calculations.incomeStatement.size)
        assertEquals(42, calculations.balanceSheet.size)
        assertEquals(49, calculations.cashFlowStatement.size)
    }
}
