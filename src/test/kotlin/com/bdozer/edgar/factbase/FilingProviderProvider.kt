package com.bdozer.edgar.factbase

import com.bdozer.xml.HttpClientExtensions.readXml
import com.bdozer.xml.XmlElement
import org.springframework.core.io.ClassPathResource

object FilingProviderProvider {

    fun tsla202010k() = object : FilingProvider {

        private val schema = ClassPathResource("factbase/tsla/tsla-20201231.xsd")
            .inputStream
            .readXml()

        private val cal = ClassPathResource("factbase/tsla/tsla-20201231_cal.xml")
            .inputStream
            .readXml()

        private val def = ClassPathResource("factbase/tsla/tsla-20201231_def.xml")
            .inputStream
            .readXml()

        private val lab = ClassPathResource("factbase/tsla/tsla-20201231_lab.xml")
            .inputStream
            .readXml()

        private val pre = ClassPathResource("factbase/tsla/tsla-20201231_pre.xml")
            .inputStream
            .readXml()

        private val instance = ClassPathResource("factbase/tsla/tsla-10k_20201231_htm.xml")
            .inputStream
            .readXml()

        override fun adsh(): String {
            return "000156459021004599"
        }

        override fun cik(): String {
            return "1318605"
        }

        override fun baseUrl(): String {
            return "https://www.sec.gov/Archives/edgar/data/1318605/000156459021004599/"
        }

        override fun inlineHtml(): String {
            return "https://www.sec.gov/ix?doc=/Archives/edgar/data/1318605/000156459021004599/tsla-10k_20201231.htm"
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
            return "tsla-20201231.xsd"
        }

        override fun calculationLinkbaseFilename(): String {
            return "tsla-20201231_cal.xml"
        }

        override fun definitionLinkbaseFilename(): String {
            return "tsla-20201231_def.xml"
        }

        override fun labelLinkbaseFilename(): String {
            return "tsla-20201231_lab.xml"
        }

        override fun presentationLinkbaseFilename(): String {
            return "tsla-20201231_pre.xml"
        }

        override fun instanceDocumentFilename(): String {
            return "tsla-20201231_htm.xml"
        }

        override fun conceptManager(): ConceptManager {
            return ConceptManager(this)
        }

        override fun labelManager(): LabelManager {
            return LabelManager(this)
        }

        override fun factsParser(): FactsParser {
            return FactsParser(this)
        }

        override fun filingArcsParser(): FilingArcsParser {
            return FilingArcsParser(this)
        }

    }

    fun dbx202010k() = object : FilingProvider {

        private val schema = ClassPathResource("factbase/dbx/dbx-20201231.xsd")
            .inputStream
            .readXml()

        private val cal = ClassPathResource("factbase/dbx/dbx-20201231_cal.xml")
            .inputStream
            .readXml()

        private val def = ClassPathResource("factbase/dbx/dbx-20201231_def.xml")
            .inputStream
            .readXml()

        private val lab = ClassPathResource("factbase/dbx/dbx-20201231_lab.xml")
            .inputStream
            .readXml()

        private val pre = ClassPathResource("factbase/dbx/dbx-20201231_pre.xml")
            .inputStream
            .readXml()

        private val instance = ClassPathResource("factbase/dbx/dbx-20201231_htm.xml")
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

        override fun conceptManager(): ConceptManager {
            return ConceptManager(this)
        }

        override fun labelManager(): LabelManager {
            return LabelManager(this)
        }

        override fun factsParser(): FactsParser {
            return FactsParser(this)
        }

        override fun filingArcsParser(): FilingArcsParser {
            return FilingArcsParser(this)
        }
    }

    fun ccl202010k() = object : FilingProvider {

        private val schema = ClassPathResource("factbase/ccl/ccl-20201130.xsd")
            .inputStream
            .readXml()

        private val cal = ClassPathResource("factbase/ccl/ccl-20201130_cal.xml")
            .inputStream
            .readXml()

        private val def = ClassPathResource("factbase/ccl/ccl-20201130_def.xml")
            .inputStream
            .readXml()

        private val lab = ClassPathResource("factbase/ccl/ccl-20201130_lab.xml")
            .inputStream
            .readXml()

        private val pre = ClassPathResource("factbase/ccl/ccl-20201130_pre.xml")
            .inputStream
            .readXml()

        private val instance = ClassPathResource("factbase/ccl/ccl-20201130_htm.xml")
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
            return "ccl-20201130.xsd"
        }

        override fun calculationLinkbaseFilename(): String {
            return "ccl-20201130_cal.xml"
        }

        override fun definitionLinkbaseFilename(): String {
            return "ccl-20201130_def.xml"
        }

        override fun labelLinkbaseFilename(): String {
            return "ccl-20201130_lab.xml"
        }

        override fun presentationLinkbaseFilename(): String {
            return "ccl-20201130_pre.xml"
        }

        override fun instanceDocumentFilename(): String {
            return "dbx-20201231_htm.xml"
        }

        override fun conceptManager(): ConceptManager {
            return ConceptManager(this)
        }

        override fun labelManager(): LabelManager {
            return LabelManager(this)
        }

        override fun factsParser(): FactsParser {
            return FactsParser(this)
        }

        override fun filingArcsParser(): FilingArcsParser {
            return FilingArcsParser(this)
        }
    }

}