package com.bdozer.edgar.factbase.filing

import com.bdozer.edgar.XbrlNamespaces.link
import com.bdozer.edgar.XbrlNamespaces.xlink
import com.bdozer.edgar.XbrlNamespaces.xsd
import com.bdozer.xml.HttpClientExtensions.readXml
import com.bdozer.xml.XmlElement
import org.apache.http.client.HttpClient
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@Service
class SECFilingFactory(
    private val http: HttpClient
) {

    fun createSECFiling(cik: String, adsh: String): SECFiling {
        val normalizedAdsh = adsh.replace("-", "")
        val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cik/$normalizedAdsh"

        val instanceDocumentFilename: String
        val instanceHtmlFilename: String

        val schemaExtensionFilename: String

        val presentationLinkbaseFilename: String
        val definitionLinkbaseFilename: String
        val labelLinkbaseFilename: String
        val calculationLinkbaseFilename: String

        /*
        read from the FilingSummary to determine the
        instance document as well as the schema document
         */
        val filingSummary = http.readXml("${baseUrl}/FilingSummary.xml")
        val files = filingSummary.getElementByTag("InputFiles")?.getElementsByTag("File")

        // find the XSD
        schemaExtensionFilename = files
            ?.find { file -> file.textContent.endsWith(".xsd") }
            ?.textContent ?: error("no schema files can be found for cik=$cik adsh=$adsh")

        /*
        find the XML instance document name
         */
        val instanceFileNode = files.find { file ->
            // in this case - we are working with more iXBRL filings with inline XBRL embedded into htm
            !file.attr("doctype").isNullOrBlank() && file.textContent.endsWith(".htm")
        }
        instanceDocumentFilename = if (instanceFileNode != null) {
            instanceFileNode.textContent.replace(".htm", "_htm.xml")
        } else {
            schemaExtensionFilename.split("\\.".toRegex(), 2).first() + ".xml"
        }

        val instanceDocument: XmlElement = http.readXml("$baseUrl/$instanceDocumentFilename")
        instanceHtmlFilename = instanceFileNode?.textContent ?: ""

        /*
        lets now read the schema XSD file
        and go ahead and derive the other files
         */
        val schema: XmlElement = http.readXml("$baseUrl/$schemaExtensionFilename")

        /*
        for some reason this sometimes gets declared at the attribute level
         */
        val linkbaseRefs = schema
            .getElementByTag(xsd, "annotation")
            ?.getElementByTag(xsd, "appinfo")
            ?.getElementsByTag(link, "linkbaseRef")
            ?.associate {
                // again for some reason this gets declared either at the document or attribute level
                val role = it.attr(xlink, "role")
                    ?: it.attr(xlink, "role")
                    ?: error("cannot find $xlink role")
                val href = it.attr(xlink, "href")
                    ?: it.attr(xlink, "href")
                    ?: error("cannot find $xlink href")
                role to href
            }
            ?: error("$schemaExtensionFilename does not define linkbaseRef")

        presentationLinkbaseFilename =
            linkbaseRefs["http://www.xbrl.org/2003/role/presentationLinkbaseRef"] ?: error("...")
        definitionLinkbaseFilename = linkbaseRefs["http://www.xbrl.org/2003/role/definitionLinkbaseRef"] ?: error("...")
        labelLinkbaseFilename = linkbaseRefs["http://www.xbrl.org/2003/role/labelLinkbaseRef"] ?: error("...")
        calculationLinkbaseFilename =
            linkbaseRefs["http://www.xbrl.org/2003/role/calculationLinkbaseRef"] ?: error("...")

        val executor = Executors.newCachedThreadPool()
        val future1 = executor.submit(Callable { http.readXml("$baseUrl/$presentationLinkbaseFilename") })
        val future2 = executor.submit(Callable { http.readXml("$baseUrl/$definitionLinkbaseFilename") })
        val future3 = executor.submit(Callable { http.readXml("$baseUrl/$labelLinkbaseFilename") })
        val future4 = executor.submit(Callable { http.readXml("$baseUrl/$calculationLinkbaseFilename") })

        val presentationLinkbase: XmlElement = future1.get()
        val definitionLinkbase: XmlElement = future2.get()
        val labelLinkbase: XmlElement = future3.get()
        val calculationLinkbase: XmlElement = future4.get()


        return SECFiling(
            adsh = adsh,
            cik = cik,
            baseUrl = baseUrl,
            inlineHtml = instanceHtmlFilename,
            schema = schema,
            calculationLinkbase = calculationLinkbase,
            definitionLinkbase = definitionLinkbase,
            labelLinkbase = labelLinkbase,
            presentationLinkbase = presentationLinkbase,
            instanceDocument = instanceDocument,
            schemaExtensionFilename = schemaExtensionFilename,
            calculationLinkbaseFilename = calculationLinkbaseFilename,
            definitionLinkbaseFilename = definitionLinkbaseFilename,
            labelLinkbaseFilename = labelLinkbaseFilename,
            presentationLinkbaseFilename = presentationLinkbaseFilename,
            instanceDocumentFilename = instanceDocumentFilename,
        )
    }
}
