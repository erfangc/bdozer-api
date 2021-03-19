package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.factbase.DBXFilingProvider.filingProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FilingParserTest {

    @Test
    internal fun parseFacts() {
        val parser = FilingParser(filingProvider = filingProvider())
        val resp = parser.parseFacts()
        assertEquals(635, resp.facts.size)
    }

}