package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.XbrlUtils
import com.starburst.starburst.edgar.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.edgar.utils.ElementExtension.targetNamespace
import com.starburst.starburst.edgar.utils.HttpClientExtensions.readLink
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import org.apache.http.impl.client.HttpClientBuilder
import org.w3c.dom.NodeList
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * The goal is to take the standard and extension XSDs of the filing
 * and determine for any given tag what it's value should be
 */
class SchemaManager(filingProvider: FilingProvider) {

    private val schemas = ConcurrentHashMap<String, Map<String, ElementDefinition>>()
    private val lookupByHref = ConcurrentHashMap<String, ElementDefinition>()
    private val linksVisited = hashSetOf<String>()

    // TODO externalize this potentially
    private val http = HttpClientBuilder.create().build()

    init {
        val extensionSchema = filingProvider.schemaExtension()
        val targetNamespace = extensionSchema.targetNamespace() ?: error("...")
        val map = extensionSchema
            .getElementsByTagNameSafe("xs:element")
            .associateByElementName(longNamespace = targetNamespace)
        schemas[targetNamespace] = map
        map.forEach { (_, value) ->
            lookupByHref["${filingProvider.schemaExtensionFilename()}#${value.id}"] = value
        }
    }

    private fun loadRemoteSchema(link: String = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd"): Map<String, ElementDefinition> {
        // TODO figure out the link of the XSD so we can properly populate it's href
        val newlyLoadedElement = XbrlUtils.readXml(
            http.readLink(link)
                ?.inputStream()
                ?: error("Unable to download link $link")
        )

        val longNamespace = newlyLoadedElement.targetNamespace()
            ?: error("schema $link does not declare a targetNamespace")

        val newlyLoadedElementDefinitions = newlyLoadedElement
            .getElementsByTagNameSafe("xs:element")
            .associateByElementName(longNamespace)
        schemas[longNamespace] = newlyLoadedElementDefinitions
        newlyLoadedElementDefinitions.forEach { (_, value) ->
            lookupByHref["$link#${value.id}"] = value
        }
        return newlyLoadedElementDefinitions
    }

    fun getElementDefinition(
        namespace: String,
        tag: String
    ): ElementDefinition? {
        if (!schemas.contains(namespace)) {
            loadRemoteSchema(namespace)
        }
        return schemas[namespace]?.get(tag)
    }

    fun getElementDefinition(
        href: String
    ): ElementDefinition? {
        val uri = URI(href)
        val link = "${uri.scheme}://${uri.host}/${uri.path}"
        if (linksVisited.contains(link)){
         loadRemoteSchema(link)
        }
        return lookupByHref[href]
    }

    private fun NodeList.associateByElementName(longNamespace: String): Map<String, ElementDefinition> {
        return this
            .toList()
            .map {
                val id = it.attributes.getNamedItem("id").textContent
                val name = it.attr("name") ?: error("name is not defined on $id in schema")
                name to
                        ElementDefinition(
                            id = id,
                            longNamespace = longNamespace,
                            name = name,
                            type = it.attributes.getNamedItem("type")?.textContent ?: "",
                            periodType = it.attributes.getNamedItem(":xbrli:periodType")?.textContent ?: "",
                        )
            }
            .toMap()
    }

}
