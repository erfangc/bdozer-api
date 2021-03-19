package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.xml.HttpClientExtensions.readXml
import com.starburst.starburst.xml.XmlElement
import org.springframework.core.io.ClassPathResource

object DBXFilingProvider {
    fun filingProvider() = object : FilingProvider {

        private val schema = ClassPathResource("factbase/dbx-20201231.xsd")
            .inputStream
            .readXml()

        private val cal = ClassPathResource("factbase/dbx-20201231_cal.xml")
            .inputStream
            .readXml()

        private val def = ClassPathResource("factbase/dbx-20201231_def.xml")
            .inputStream
            .readXml()

        private val lab = ClassPathResource("factbase/dbx-20201231_lab.xml")
            .inputStream
            .readXml()

        private val pre = ClassPathResource("factbase/dbx-20201231_pre.xml")
            .inputStream
            .readXml()

        private val instance = ClassPathResource("factbase/dbx-20201231_htm.xml")
            .inputStream
            .readXml()

        override fun adsh(): String {
            return "000146762321000012"
        }

        override fun cik(): String {
            return "1467623"
        }

        override fun baseUrl(): String {
            return "https://www.sec.gov/Archives/edgar/data/1467623/000146762321000012/"
        }

        override fun inlineHtml(): String {
            return "https://www.sec.gov/ix?doc=/Archives/edgar/data/1467623/000146762321000012/dbx-20201231.htm"
        }

        override fun schema(): XmlElement {
            return schema
        }

        override fun calculationLinkbase(): XmlElement {
            return cal
        }

        override fun definitionLinkbase(): XmlElement {
            return def
        }

        override fun labelLinkbase(): XmlElement {
            return lab
        }

        override fun presentationLinkbase(): XmlElement {
            return pre
        }

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