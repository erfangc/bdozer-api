package com.bdozer.edgar.factbase.ingestor

import com.bdozer.edgar.factbase.FilingProviderProvider.dbx202010k
import com.bdozer.edgar.factbase.FactsParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FactsParserTest {

    @Test
    internal fun parseFacts() {
        val parser = FactsParser(filingProvider = dbx202010k())
        val resp = parser.parseFacts()
        assertEquals(635, resp.facts.size)
    }

}