package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.dataclasses.MetaLink
import org.w3c.dom.Element

interface FilingProvider {
    fun adsh(): String
    fun cik(): String

    fun baseUrl(): String
    fun metaLink(): MetaLink
    fun formType(): String

    fun schemaExtension(): Element
    fun calculationLinkbase(): Element
    fun definitionLinkbase(): Element
    fun labelLinkbase(): Element
    fun presentationLinkbase(): Element
    fun instanceDocument(): Element

    fun schemaExtensionFilename(): String
    fun calculationLinkbaseFilename(): String
    fun definitionLinkbaseFilename(): String
    fun labelLinkbaseFilename(): String
    fun presentationLinkbaseFilename(): String
    fun instanceDocumentFilename(): String

}
