package com.bdozer.edgar.factbase.ingestor

import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
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