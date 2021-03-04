package com.starburst.starburst.xbrl.factbase

import com.starburst.starburst.xbrl.FilingProvider
import com.starburst.starburst.xbrl.dataclasses.ElementDefinition
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils
import com.starburst.starburst.xbrl.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.xbrl.utils.ElementExtension.targetNamespace
import com.starburst.starburst.xbrl.utils.HttpClientExtensions.readLink
import com.starburst.starburst.xbrl.utils.NodeListExtension.attr
import com.starburst.starburst.xbrl.utils.NodeListExtension.toList
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import org.w3c.dom.NodeList
import java.util.concurrent.ConcurrentHashMap

/**
 * The goal is to take the standard and extension XSDs of the filing
 * and determine for any given tag what it's value should be
 */
class SchemaManager(filingProvider: FilingProvider) {

    private val schemas = ConcurrentHashMap<String, Map<String, ElementDefinition>>()
    private val log = LoggerFactory.getLogger(SchemaManager::class.java)

    // TODO externalize this potentially
    private val http = HttpClientBuilder.create().build()

    init {
        val extensionSchema = filingProvider.schemaExtension()
        val targetNamespace = extensionSchema.targetNamespace() ?: error("...")
        schemas[targetNamespace] = extensionSchema
            .getElementsByTagNameSafe("xs:element")
            .associateByElementName(namespace = targetNamespace)
    }

    private fun loadGaapSchema(namespace: String): Map<String, ElementDefinition> {
        val schema = schemas[namespace]
        return if (schema == null) {
            log.info("Retrieving US Gaap schema for $namespace")
            val link = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd"
            val newlyLoadedElement = XbrlUtils.readXml(
                http.readLink(link)
                    ?.inputStream()
                    ?: error("Unable to download link $link")
            )
            // TODO actually retrieve the correct us-gaap schema document instead of this hard coded one
            val map = newlyLoadedElement
                .getElementsByTagNameSafe("xs:element")
                .associateByElementName(namespace)
            schemas[namespace] = map
            map
        } else {
            schema
        }
    }

    fun getElementDefinition(
        namespace: String,
        tag: String
    ): ElementDefinition? {
        if (!schemas.contains(namespace)) {
            loadGaapSchema(namespace)
        }
        return schemas[namespace]?.get(tag)
    }

    private fun NodeList.associateByElementName(namespace: String): Map<String, ElementDefinition> {
        return this
            .toList()
            .map {
                val id = it.attributes.getNamedItem("id").textContent
                val name = it.attr("name") ?: error("name is not defined on $id in schema")
                name to
                        ElementDefinition(
                            id = id,
                            namespace = namespace,
                            name = name,
                            type = it.attributes.getNamedItem("type")?.textContent ?: "",
                            periodType = it.attributes.getNamedItem(":xbrli:periodType")?.textContent ?: "",
                        )
            }
            .toMap()
    }


}
