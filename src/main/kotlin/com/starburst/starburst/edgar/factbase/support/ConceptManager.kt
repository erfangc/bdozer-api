package com.starburst.starburst.edgar.factbase.support

import com.starburst.starburst.edgar.ConceptDefinitions
import com.starburst.starburst.edgar.ConceptDefinitions.getBySchemaLocationAndName
import com.starburst.starburst.edgar.ConceptDefinitions.putSchemaDocument
import com.starburst.starburst.edgar.XbrlNamespaces.xsd
import com.starburst.starburst.edgar.dataclasses.ConceptDefinition
import com.starburst.starburst.edgar.FilingProvider

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
 * since [ConceptManager] is agnostic of how local namespaces are declared in the Instance document
 * [ConceptManager] require full namespace
 */
class ConceptManager(filingProvider: FilingProvider) {

    private val schemaFileName = filingProvider.schemaExtensionFilename()
    private val schemaLocations: Map<String, String>
    private val schema = filingProvider.schema()

    init {
        /*
        load all the schemas and imports
         */
        val targetNamespace = schema.targetNamespace()
            ?: error("target namespace not declared on $schemaFileName")

        schemaLocations = schema
            .getElementsByTag(xsd, "import")
            .associate {
                val namespace = it.attr("namespace")
                    ?: error("no namespace defined on import")
                val schemaLocation = it.attr("schemaLocation")
                    ?: error("no schema location defined for namespace $namespace on import")
                namespace to schemaLocation
            }.plus(targetNamespace to schemaFileName)

        putSchemaDocument(schemaFileName, filingProvider.schema())
    }

    fun getConceptDefinition(conceptHref: String) = ConceptDefinitions.getConceptDefinition(conceptHref)

    fun getConceptDefinition(namespace: String, conceptName: String): ConceptDefinition? {
        val schemaLocation = schemaLocations[namespace] ?: return null
        return getBySchemaLocationAndName(schemaLocation, conceptName)
    }


}