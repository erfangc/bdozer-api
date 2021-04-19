package com.bdozer.sec.factbase.modelbuilder

import com.bdozer.sec.dataclasses.XbrlExplicitMember
import com.bdozer.sec.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.sec.factbase.dataclasses.Fact
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class ItemNameGeneratorTest {

    @Test
    fun itemName() {
    }

    @Test
    fun testItemName() {
        val itemNameGenerator = ItemNameGenerator()
        val result = itemNameGenerator.itemName(fact())
        assertEquals(
            "RevenueWithALongTagName_srt_SomeOtherAxis_ticker_AnotherValue_srt_SomeAxis_us_gaap_Value",
            result
        )
    }

    private fun fact(): Fact {
        return Fact(
            _id = "",
            instanceDocumentElementId = "",
            instanceDocumentElementName = "",
            cik = "",
            adsh = "",
            entityName = "",
            primarySymbol = "",
            formType = "",
            conceptName = "RevenueWithALongTagName",
            conceptHref = "",
            namespace = "",
            instant = LocalDate.now(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
            documentFiscalYearFocus = 2020,
            documentPeriodEndDate = LocalDate.now(),
            explicitMembers = listOf(
                XbrlExplicitMember(dimension = "srt_SomeAxis", value = "us-gaap:Value"),
                XbrlExplicitMember(dimension = "srt_SomeOtherAxis", value = "ticker:AnotherValue"),
            ),
            sourceDocument = "",
            stringValue = "",
            lastUpdated = Instant.now().toString(),
        )
    }
}