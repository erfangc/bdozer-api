package com.bdozer.sec.factbase.ingestor

import com.bdozer.api.common.dataclasses.sec.DocumentFiscalPeriodFocus
import com.bdozer.xml.XmlElement
import java.time.LocalDate

object InstanceDocumentExtensions {
    fun XmlElement.documentFiscalYearFocus(): Int {
        val found = getElementsByTag("dei:DocumentFiscalYearFocus")
        if (found.isEmpty()) {
            return 0
        }
        return found
            .first()
            .textContent
            .toIntOrNull() ?: 0
    }

    fun XmlElement.tradingSymbol(): String {
        val found = getElementsByTag("dei:TradingSymbol")
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    fun XmlElement.entityRegistrantName(): String {
        val found = getElementsByTag("dei:EntityRegistrantName")
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    fun XmlElement.documentFiscalPeriodFocus(): DocumentFiscalPeriodFocus {
        val found = getElementsByTag("dei:DocumentFiscalPeriodFocus")
        if (found.isEmpty()) {
            return DocumentFiscalPeriodFocus.NA
        }
        return found
            .first()
            .textContent?.let { DocumentFiscalPeriodFocus.valueOf(it) } ?: DocumentFiscalPeriodFocus.NA
    }

    fun XmlElement.documentPeriodEndDate(): LocalDate? {
        val found = getElementsByTag("dei:DocumentPeriodEndDate")
        if (found.isEmpty()) {
            return null
        }
        return found
            .first()
            .textContent
            ?.let { date -> LocalDate.parse(date) }
    }

    fun XmlElement.formType(): String {
        return getElementByTag("dei:DocumentType")
            ?.textContent ?: "Unknown"
    }
}