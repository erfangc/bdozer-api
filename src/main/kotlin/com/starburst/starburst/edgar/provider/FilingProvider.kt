package com.starburst.starburst.edgar.provider

import com.starburst.starburst.edgar.XmlElement

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

}