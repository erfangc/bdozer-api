package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.xml.XmlElement
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.xml.XbrlUtils
import com.starburst.starburst.edgar.factbase.ingestor.FilingParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

internal class FilingParserTest {

    @Test
    fun parseFacts() {
        val obj = FilingParser(filingProvider = filingProvider())
        val response = obj.parseFacts()
        assertEquals(1330, response.facts.size)
    }

    private fun filingProvider() = object : FilingProvider {

        override fun adsh(): String {
            return "000146762321000012"
        }

        override fun cik(): String {
            return "1467623"
        }

        override fun baseUrl(): String {
            return "https://www.sec.gov/Archives/edgar/data/"
        }

        override fun inlineHtml(): String {
            return "..."
        }

        fun readFile(name: String): XmlElement {
            println("reading $name")
            val istream = ClassPathResource(name).inputStream.readAllBytes().inputStream()
            return XbrlUtils.readXml(istream)
        }

        private val schemaExtension = readFile("factbase/dbx-20201231.xsd")

        override fun schema(): XmlElement {
            return schemaExtension
        }

        private val calculation = readFile("factbase/dbx-20201231_cal.xml")

        override fun calculationLinkbase(): XmlElement {
            return calculation
        }

        private val definition = readFile("factbase/dbx-20201231_def.xml")

        override fun definitionLinkbase(): XmlElement {
            return definition
        }

        private val label = readFile("factbase/dbx-20201231_lab.xml")

        override fun labelLinkbase(): XmlElement {
            return label
        }

        private val presentation = readFile("factbase/dbx-20201231_pre.xml")

        override fun presentationLinkbase(): XmlElement {
            return presentation
        }

        private val instance = readFile("factbase/dbx-20201231_htm.xml")

        override fun instanceDocument(): XmlElement {
            return instance
        }

        override fun schemaExtensionFilename(): String {
            return "dbx-20201231.xsd"
        }

        override fun calculationLinkbaseFilename(): String {
            return "dbx-20201231_cal.xml"
        }

        override fun definitionLinkbaseFilename(): String {
            return "dbx-20201231_def.xml"
        }

        override fun labelLinkbaseFilename(): String {
            return "dbx-20201231_lab.xml"
        }

        override fun presentationLinkbaseFilename(): String {
            return "dbx-20201231_pre.xml"
        }

        override fun instanceDocumentFilename(): String {
            return "dbx-20201231_htm.xml"
        }
    }
}
