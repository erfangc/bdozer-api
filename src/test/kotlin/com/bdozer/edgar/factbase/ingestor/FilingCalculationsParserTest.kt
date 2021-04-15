package com.bdozer.edgar.factbase.ingestor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.bdozer.AppConfiguration
import com.bdozer.edgar.factbase.FilingProviderProvider.dbx202010k
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.edgar.factbase.ingestor.support.FilingCalculationsParser
import com.bdozer.edgar.factbase.support.FilingConceptsHolder
import com.bdozer.edgar.provider.FilingProviderFactory
import com.bdozer.extensions.DoubleExtensions.orZero
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.litote.kmongo.and
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import kotlin.system.exitProcess

internal class FilingCalculationsParserTest {
    @Test
    fun parseStatements() {
        val parser = FilingCalculationsParser(filingProvider = dbx202010k())
        val calculations = parser.parseCalculations()
        assertEquals(18, calculations.incomeStatement.size)
        assertEquals(42, calculations.balanceSheet.size)
        assertEquals(49, calculations.cashFlowStatement.size)
    }
}
