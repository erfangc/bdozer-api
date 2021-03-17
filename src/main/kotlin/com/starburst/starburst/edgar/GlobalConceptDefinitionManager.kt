package com.starburst.starburst.edgar

import com.starburst.starburst.edgar.XbrlNamespaces.xsd
import com.starburst.starburst.edgar.dataclasses.ConceptDefinition
import com.starburst.starburst.xml.HttpClientExtensions.readXml
import com.starburst.starburst.xml.XmlElement
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * This is an App wide [ConceptDefinition] manager
 * that allows a clean interface for retrieving concept definitions
 * by their `href`
 */
object GlobalConceptDefinitionManager {

    private val schemaLocations = mutableMapOf<String, String>()
    private val concepts = mutableMapOf<String, Map<String, ConceptDefinition>>()
    private val conceptsByName = mutableMapOf<String, Map<String, ConceptDefinition>>()
    private val http = HttpClientBuilder.create().build()
    private val visitedSchemaLocations = hashSetOf<String>()
    private val log = LoggerFactory.getLogger(GlobalConceptDefinitionManager::class.java)

    fun getConceptDefinition(href: String): ConceptDefinition? {
        val uri = URI(href)
        val schemaLocation = schemaLocation(uri)
        if (!visitedSchemaLocations.contains(schemaLocation)) {
            loadRemoteSchema(schemaLocation)
        }
        val conceptId = uri.fragment
        return concepts[schemaLocation]?.get(conceptId)
    }

    private fun schemaLocation(uri: URI) = if (uri.host == null) {
        uri.path
    } else {
        "${uri.scheme}://${uri.host}${uri.path}"
    }

    fun getBySchemaLocationAndName(schemaLocation: String, conceptName: String): ConceptDefinition? {
        if (!visitedSchemaLocations.contains(schemaLocation))
            loadRemoteSchema(schemaLocation)
        return conceptsByName[schemaLocation]?.get(conceptName)
    }

    fun putSchemaDocument(schemaLocation: String, schemaDocument: XmlElement) {
        val targetNamespace = schemaDocument.targetNamespace() ?: error("$schemaLocation has no target namespace")
        schemaLocations[targetNamespace] = schemaLocation
        val conceptDefinitions = schemaDocument
            .getElementsByTag(xsd, "element")
            .map { element ->
                val name = element.attr("name") ?: error("")
                val id = element.attr("id")
                ConceptDefinition(
                    id = id ?: name,
                    targetNamespace = targetNamespace,
                    conceptHref = "$schemaLocation#$id",
                    conceptName = name,
                    type = element.attr("type"),
                    abstract = element.attr("abstract").toBoolean(),
                    periodType = element.attr(XbrlNamespaces.xbrli, "periodType"),
                    nillable = element.attr("nillable").toBoolean(),
                    substitutionGroup = element.attr("substitutionGroup"),
                    balance = element.attr(XbrlNamespaces.xbrli, "balance"),
                )
            }
        concepts[schemaLocation] = conceptDefinitions.associateBy { it.id }
        conceptsByName[schemaLocation] = conceptDefinitions.associateBy { it.conceptName }
        visitedSchemaLocations.add(schemaLocation)
        log.info("Finished caching ${conceptDefinitions.size} concepts from $schemaLocation")
    }

    private fun loadRemoteSchema(schemaLocation: String) {
        val schemaDocument = http.readXml(schemaLocation)
        putSchemaDocument(schemaLocation, schemaDocument)
    }

}