package com.starburst.starburst.edgar.provider

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.utils.HttpClientExtensions.readXml
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import org.apache.http.client.HttpClient
import org.springframework.stereotype.Service

@Service
class FilingProviderFactory(
    private val http: HttpClient
) {
    fun createFilingProvider(cik: String, adsh: String): FilingProvider {
        val normalizedAdsh = adsh.replace("-", "")
        val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cik/$normalizedAdsh"

        val instanceFilename: String
        val instanceHtmlFilename: String

        val schemaFilename: String

        val presentationLinkbaseRef: String
        val definitionLinkbaseRef: String
        val labelLinkbaseRef: String
        val calculationLinkbaseRef: String

        /*
        read from the FilingSummary to determine the
        instance document as well as the schema document
         */
        val filingSummary = http.readXml("${baseUrl}/FilingSummary.xml")
        val files = filingSummary.getElementByTag("InputFiles")?.getElementsByTag("File")

        // find the XSD
        schemaFilename = files
            ?.find { file -> file.textContent.endsWith(".xsd") }
            ?.textContent ?: error("...")

        // find the XML instance document name
        val instanceFileNode = files.find { file ->
            // in this case - we are working with more iXBRL filings with inline XBRL embedded into htm
            !file.attr("doctype").isNullOrBlank() && file.textContent.endsWith(".htm")
        }
        instanceFilename = if (instanceFileNode != null) {
            instanceFileNode.textContent.replace(".htm", "_htm.xml")
        } else {
            schemaFilename.split("\\.".toRegex(), 2).first() + ".xml"
        }
        val instanceDocument: XmlElement = http.readXml("$baseUrl/$instanceFilename")
        instanceHtmlFilename = instanceFileNode?.textContent ?: error("...")

        // lets now read the schema XSD file
        // and go ahead and derive the other files
        val schema: XmlElement = http.readXml("$baseUrl/$schemaFilename")
        val xsd = schema.getShortNamespace(longNamespace = "http://www.w3.org/2001/XMLSchema")
        val link = schema.getShortNamespace(longNamespace = "http://www.xbrl.org/2003/linkbase")
        val xlink = schema.getShortNamespace(longNamespace = "http://www.w3.org/1999/xlink")

        val linkbaseRefs = schema
            .getElementByTag("${xsd}annotation")
            ?.getElementByTag("${xsd}appinfo")
            ?.getElementsByTag("${link}linkbaseRef")
            ?.associate { it.attr("${xlink}role")!! to it.attr("${xlink}href")!! }
            ?: error("$schemaFilename does not define linkbaseRef")

        presentationLinkbaseRef = linkbaseRefs["http://www.xbrl.org/2003/role/presentationLinkbaseRef"] ?: error("...")
        definitionLinkbaseRef = linkbaseRefs["http://www.xbrl.org/2003/role/definitionLinkbaseRef"] ?: error("...")
        labelLinkbaseRef = linkbaseRefs["http://www.xbrl.org/2003/role/labelLinkbaseRef"] ?: error("...")
        calculationLinkbaseRef = linkbaseRefs["http://www.xbrl.org/2003/role/calculationLinkbaseRef"] ?: error("...")

        val presentationLinkbase: XmlElement = http.readXml("$baseUrl/$presentationLinkbaseRef")
        val definitionLinkbase: XmlElement = http.readXml("$baseUrl/$definitionLinkbaseRef")
        val labelLinkbase: XmlElement = http.readXml("$baseUrl/$labelLinkbaseRef")
        val calculationLinkbase: XmlElement = http.readXml("$baseUrl/$calculationLinkbaseRef")

        return object : FilingProvider {

            override fun adsh(): String {
                return adsh
            }

            override fun cik(): String {
                return cik
            }

            override fun baseUrl(): String {
                return baseUrl
            }

            override fun inlineHtml(): String {
                return instanceHtmlFilename
            }

            override fun schema(): XmlElement {
                return schema
            }

            override fun calculationLinkbase(): XmlElement {
                return calculationLinkbase
            }

            override fun definitionLinkbase(): XmlElement {
                return definitionLinkbase
            }

            override fun labelLinkbase(): XmlElement {
                return labelLinkbase
            }

            override fun presentationLinkbase(): XmlElement {
                return presentationLinkbase
            }

            override fun instanceDocument(): XmlElement {
                return instanceDocument
            }

            override fun schemaExtensionFilename(): String {
                return schemaFilename
            }

            override fun calculationLinkbaseFilename(): String {
                return calculationLinkbaseRef
            }

            override fun definitionLinkbaseFilename(): String {
                return definitionLinkbaseRef
            }

            override fun labelLinkbaseFilename(): String {
                return labelLinkbaseRef
            }

            override fun presentationLinkbaseFilename(): String {
                return presentationLinkbaseRef
            }

            override fun instanceDocumentFilename(): String {
                return instanceFilename
            }

        }
    }
}