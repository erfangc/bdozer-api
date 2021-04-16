package com.bdozer.edgar.provider

import com.bdozer.edgar.factbase.FactsParser
import com.bdozer.edgar.factbase.FilingCalculationsParser
import com.bdozer.xml.XmlElement

interface FilingProvider {
    fun adsh(): String
    fun cik(): String

    fun baseUrl(): String

    fun inlineHtml(): String
    fun schema(): XmlElement
    fun calculationLinkbase(): XmlElement
    fun definitionLinkbase(): XmlElement
    fun labelLinkbase(): XmlElement
    fun presentationLinkbase(): XmlElement
    fun instanceDocument(): XmlElement

    fun schemaExtensionFilename(): String
    fun calculationLinkbaseFilename(): String
    fun definitionLinkbaseFilename(): String
    fun labelLinkbaseFilename(): String
    fun presentationLinkbaseFilename(): String
    fun instanceDocumentFilename(): String

    fun conceptManager(): ConceptManager
    fun labelManager(): LabelManager
    fun factsParser(): FactsParser
    fun filingCalculationsParser(): FilingCalculationsParser
}