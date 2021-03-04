package com.starburst.starburst.xbrl.factbase

import com.starburst.starburst.xbrl.FilingProvider
import com.starburst.starburst.xbrl.dataclasses.MetaLink
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.w3c.dom.Element

internal class XbrlFactParserTest {

    @Test
    fun parseFacts() {
        val obj = XbrlFactParser(filingProvider = filingProvider())
        val facts = obj.parseFacts()
        assertEquals(1330, facts.size)
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

        override fun metaLink(): MetaLink {
            TODO("Not yet implemented")
        }

        override fun formType(): String {
            return "10-K"
        }

        fun readFile(name: String): Element {
            println("reading $name")
            val istream = ClassPathResource(name).inputStream.readAllBytes().inputStream()
            return XbrlUtils.readXml(istream)
        }

        private val schemaExtension = readFile("factbase/dbx-20201231.xsd")

        override fun schemaExtension(): Element {
            return schemaExtension
        }

        private val calculation = readFile("factbase/dbx-20201231_cal.xml")

        override fun calculationLinkbase(): Element {
            return calculation
        }

        private val definition = readFile("factbase/dbx-20201231_def.xml")

        override fun definitionLinkbase(): Element {
            return definition
        }

        private val label = readFile("factbase/dbx-20201231_lab.xml")

        override fun labelLinkbase(): Element {
            return label
        }

        private val presentation = readFile("factbase/dbx-20201231_pre.xml")

        override fun presentationLinkbase(): Element {
            return presentation
        }

        private val instance = readFile("factbase/dbx-20201231_htm.xml")

        override fun instanceDocument(): Element {
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
