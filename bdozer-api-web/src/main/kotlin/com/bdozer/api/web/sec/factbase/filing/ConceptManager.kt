package com.bdozer.api.web.sec.factbase.filing

import com.bdozer.api.web.sec.UniversalConceptsManager
import com.bdozer.api.web.sec.UniversalConceptsManager.getBySchemaLocationAndName
import com.bdozer.api.web.sec.UniversalConceptsManager.putSchemaDocument
import com.bdozer.api.web.sec.XbrlNamespaces.xsd
import com.bdozer.api.common.dataclasses.sec.Concept

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
class ConceptManager(SECFiling: SECFiling) {

    private val schemaFilename = SECFiling.schemaExtensionFilename
    private val schemaLocations: Map<String, String>
    private val schema = SECFiling.schema

    init {
        /*
        load all the schemas and imports
         */
        val targetNamespace = schema.targetNamespace()
            ?: error("target namespace not declared on $schemaFilename")

        schemaLocations = schema
            .getElementsByTag(xsd, "import")
            .associate {
                val namespace = it.attr("namespace")
                    ?: error("no namespace defined on import")
                val schemaLocation = it.attr("schemaLocation")
                    ?: error("no schema location defined for namespace $namespace on import")
                namespace to schemaLocation
            }.plus(targetNamespace to schemaFilename)

        putSchemaDocument(schemaFilename, SECFiling.schema)
    }

    fun getConcept(conceptHref: String) = UniversalConceptsManager.getConcept(conceptHref)

    fun getConcept(namespace: String, conceptName: String): Concept? {
        val schemaLocation = schemaLocations[namespace] ?: return null
        return getBySchemaLocationAndName(schemaLocation, conceptName)
    }

}
