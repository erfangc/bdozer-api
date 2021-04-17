package com.bdozer.edgar.factbase

import com.bdozer.edgar.factbase.dataclasses.Arc
import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.xml.XmlElement

interface FilingProvider {
    fun adsh(): String
    fun cik(): String

    fun baseUrl(): String

    fun inlineHtml(): String
    fun schema(): XmlElement
    fun calculationLinkbase(): XmlElement
    fun definitionLinkbase(): XmlElement
    fun labelLinkbase(): XmlElement
    fun presentationLinkbase(): XmlElement
    fun instanceDocument(): XmlElement

    fun schemaExtensionFilename(): String
    fun calculationLinkbaseFilename(): String
    fun definitionLinkbaseFilename(): String
    fun labelLinkbaseFilename(): String
    fun presentationLinkbaseFilename(): String
    fun instanceDocumentFilename(): String

    fun conceptManager(): ConceptManager
    fun labelManager(): LabelManager
    fun factsParser(): FactsParser
    fun filingArcsParser(): FilingArcsParser

    fun incomeStatementDeclaredDimensions(): List<Dimension> {
        return declaredDimensions(filingArcsParser().parseFilingArcs().incomeStatement)
    }

    /**
     * Return the declared dimensions of a given set statement represented as [Arc]
     * Each returned [Dimension] contains the dimension axis declaration as well as all
     * members of that dimension
     *
     * @param statementArcs adding statement arcs
     */
    fun declaredDimensions(statementArcs: List<Arc>): List<Dimension> {
        /*
        dimensional arcs are any thing in the StatementTable
        immediate child that is not an statement item
         */
        val dimensionsArcs = statementArcs
            .filter {
                // TODO figure out if relying on StatementTable children as dimension declarations is stable
                it.parentHref?.endsWith("StatementTable") == true
                        && !it.conceptHref.endsWith("StatementLineItems")
            }
        val instanceDocument = instanceDocument()
        val conceptManager = conceptManager()

        return dimensionsArcs.map { dimension ->
            val dimensionHref = dimension.conceptHref
            val dimensionConcept = conceptManager.getConcept(dimensionHref) ?: error("$dimensionHref cannot be found")
            val domainMembers = statementArcs.filter { it.parentHref == dimensionHref }

            /*
            find the member concepts along the current dimension in the declaring statement arcs
             */
            val memberConcepts = domainMembers
                .flatMap { domainMember ->
                    /*
                    we flat map through "domain" members, as it is a bit of a unnecessary intermediate layers
                    that separate member declaration from grandparent dimension declaration
                     */
                    val conceptHref = domainMember.conceptHref
                    statementArcs.filter { it.parentHref == conceptHref }.map { dimensionMember ->
                        val concept = conceptManager.getConcept(dimensionMember.conceptHref)
                            ?: error("${dimensionMember.conceptHref} cannot be found")
                        val ns = instanceDocument.getShortNamespace(concept.targetNamespace)
                            ?: error("target namespace ${concept.targetNamespace} cannot be found")
                        "$ns:${concept.conceptName}"
                    }
                }.toSet()

            val ns = instanceDocument.getShortNamespace(dimensionConcept.targetNamespace)
                ?: error("target namespace ${dimensionConcept.targetNamespace} cannot be found")

            Dimension(
                dimensionConcept = "$ns:${dimensionConcept.conceptName}",
                memberConcepts = memberConcepts,
            )
        }

    }

}