package com.starburst.starburst.xbrl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.starburst.starburst.xbrl.dataclasses.MetaLink
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils.readXml
import com.starburst.starburst.xbrl.utils.HttpClientExtensions.readLink
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.stereotype.Service
import org.w3c.dom.Element

class FilingProviderImpl(
    private val cik: String,
    private val adsh: String,
    http: HttpClient = HttpClientBuilder.create().build(),
    objectMapper: ObjectMapper = jacksonObjectMapper()
) : FilingProvider {

    private val normalizedAdsh = adsh.replace("-", "")
    private val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cik/$normalizedAdsh"
    private val metaLinkGetRequest = HttpGet("${baseUrl}/MetaLinks.json")

    val metaLink = try {
        objectMapper
            .readValue<MetaLink>(http.execute(metaLinkGetRequest).entity.content)
    } catch (e: Exception) {
        error("Unable to find MetaLinks for adsh=$normalizedAdsh cik=$cik")
    }

    init {
        metaLinkGetRequest.releaseConnection()
    }

    private val instance = metaLink.instance.entries.first()
    private val dts = instance.value.dts

    private val instanceDocumentFileName = dts.inline.local.first().replace(".htm", "_htm.xml")
    private val schemaExtensionFilename = dts.schema.local.first()
    private val calculationFileName = dts.calculationLink.local.first()
    private val labelFilename = dts.labelLink.local.first()
    private val definitionFileName = dts.definitionLink.local.first()
    private val presentationFileName = dts.presentationLink.local.first()

    //
    // resolve the locations of the XBRL
    // files based on the information above
    //
    private val instanceDocument = readXml(http.readLink("${baseUrl}/$instanceDocumentFileName")?.inputStream() ?: error("..."))
    private val schemaExtension = readXml(http.readLink("${baseUrl}/$schemaExtensionFilename")?.inputStream() ?: error("..."))
    private val labelLinkbase = readXml(http.readLink("${baseUrl}/$labelFilename")?.inputStream() ?: error("..."))
    private val definitionLinkbase = readXml(http.readLink("${baseUrl}/$definitionFileName")?.inputStream() ?: error("..."))
    private val presentationLinkbase = readXml(http.readLink("${baseUrl}/$presentationFileName")?.inputStream() ?: error("..."))
    private val calculationLinkbase = readXml(http.readLink("${baseUrl}/$calculationFileName")?.inputStream() ?: error("..."))

    override fun adsh(): String {
        return this.adsh
    }

    override fun cik(): String {
        return this.cik
    }

    override fun baseUrl(): String {
        return this.baseUrl
    }

    override fun metaLink(): MetaLink {
        return this.metaLink
    }

    override fun formType(): String {
        TODO("Not yet implemented")
    }

    override fun inlineHtml(): String {
        return dts.inline.local.first()
    }

    override fun schemaExtension(): Element {
        return schemaExtension
    }

    override fun calculationLinkbase(): Element {
        return calculationLinkbase
    }

    override fun definitionLinkbase(): Element {
        return definitionLinkbase
    }

    override fun labelLinkbase(): Element {
        return labelLinkbase
    }

    override fun presentationLinkbase(): Element {
        return presentationLinkbase
    }

    override fun instanceDocument(): Element {
        return instanceDocument
    }

    override fun schemaExtensionFilename(): String {
        return schemaExtensionFilename
    }

    override fun calculationLinkbaseFilename(): String {
        return calculationFileName
    }

    override fun definitionLinkbaseFilename(): String {
        return definitionFileName
    }

    override fun labelLinkbaseFilename(): String {
        return labelFilename
    }

    override fun presentationLinkbaseFilename(): String {
        return presentationFileName
    }

    override fun instanceDocumentFilename(): String {
        return instanceDocumentFileName
    }
}
