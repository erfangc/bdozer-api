package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.factbase.DBXFilingProvider.filingProvider
import com.starburst.starburst.edgar.factbase.ingestor.support.FactsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FactsParserTest {

    @Test
    internal fun parseFacts() {
        val parser = FactsParser(filingProvider = filingProvider())
        val resp = parser.parseFacts()
        assertEquals(635, resp.facts.size)
    }

}