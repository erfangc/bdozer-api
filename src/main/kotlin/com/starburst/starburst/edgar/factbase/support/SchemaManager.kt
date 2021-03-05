package com.starburst.starburst.edgar.factbase.support

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.utils.HttpClientExtensions.readXml
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import org.apache.http.impl.client.HttpClientBuilder
import org.w3c.dom.NodeList
import java.net.URI

/**
 * The goal is to take the standard and extension XSDs of the filing
 * and determine for any given tag what it's value should be
 *
 * There are two ways that an element from the Instance document will be referenced
 *
 * 1. From outside the Instance document itself via an href - which contains the local or remote location
 * of the schema XSD as well as the `id` attribute of the element definition within that XSD
 *
 * 2. From inside the Instance document, in which case the namespace / local namespace is defined
 * since [SchemaManager] is agnostic of how local namespaces are declared in the Instance document
 * [SchemaManager] require full namespace
 */
class SchemaManager(filingProvider: FilingProvider) {

    // TODO externalize this potentially
    private val http = HttpClientBuilder.create().build()
    private val elementDefinitionsByLongNamespace = mutableMapOf<String, Map<String, ElementDefinition>>()
    private val elementDefinitionsByHref = mutableMapOf<String, ElementDefinition>()
    private val namespaceToSchemaLocation: Map<String, String>
    private val linksVisited = hashSetOf<String>()
    private val schema = filingProvider.schema()
    private val schemaFileName = filingProvider.schemaExtensionFilename()
    private val prefix = schema.getShortNamespace("http://www.w3.org/2001/XMLSchema")

    init {
        /*
        load all the schemas and imports
         */
        namespaceToSchemaLocation = schema.getElementsByTag("${prefix}import").associate {
            it.attr("namespace")!! to it.attr("schemaLocation")!!
        }
        linksVisited.add(schemaFileName)
        processNewSchemaIntoState(schema, schema.targetNamespace()!!, schemaFileName)
    }

    private fun loadRemoteSchema(schemaLocation: String): Map<String, ElementDefinition> {
        val newlyLoadedElement = http.readXml(schemaLocation)
        val namespace = newlyLoadedElement.targetNamespace() ?: error("$schemaLocation has no target namespace")
        return processNewSchemaIntoState(newlyLoadedElement, namespace, schemaLocation)
    }

    private fun loadRemoteSchemaByNamespace(namespace: String): Map<String, ElementDefinition> {
        val schemaLocation = namespaceToSchemaLocation[namespace]
            ?: error("cannot find a schema location for $namespace")
        val schema = http.readXml(schemaLocation)
        return processNewSchemaIntoState(schema, namespace, schemaLocation)
    }

    private fun processNewSchemaIntoState(
        schema: XmlElement,
        namespace: String,
        schemaLocation: String
    ): Map<String, ElementDefinition> {
        val xsd = schema.getShortNamespace("http://www.w3.org/2001/XMLSchema")
        val newlyLoadedElementDefinitions = schema
            .getElementsByTagName("${xsd}element")
            .associateByElementName(namespace)

        linksVisited.add(schemaLocation)
        elementDefinitionsByLongNamespace[namespace] = newlyLoadedElementDefinitions
        newlyLoadedElementDefinitions.forEach { (_, value) ->
            elementDefinitionsByHref["$schemaLocation#${value.id}"] = value
        }
        return newlyLoadedElementDefinitions
    }

    fun getElementDefinition(
        namespace: String,
        nodeName: String
    ): ElementDefinition? {
        if (!elementDefinitionsByLongNamespace.contains(namespace)) {
            loadRemoteSchemaByNamespace(namespace)
        }
        return elementDefinitionsByLongNamespace[namespace]?.get(nodeName)
    }

    fun getElementDefinition(
        href: String
    ): ElementDefinition? {
        val uri = URI(href)
        val link = "${uri.scheme}://${uri.host}/${uri.path}"
        if (linksVisited.contains(link)){
         loadRemoteSchema(link)
        }
        return elementDefinitionsByHref[href]
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
