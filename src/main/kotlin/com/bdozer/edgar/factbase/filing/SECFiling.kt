package com.bdozer.edgar.factbase.filing

import com.bdozer.edgar.factbase.FactsParser
import com.bdozer.edgar.factbase.dataclasses.Arc
import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.edgar.factbase.itemgenerator.ItemGenerator
import com.bdozer.xml.XmlElement

class SECFiling(
    val adsh: String,
    val cik: String,
    val baseUrl: String,
    val inlineHtml: String,
    val schema: XmlElement,
    val calculationLinkbase: XmlElement,
    val definitionLinkbase: XmlElement,
    val labelLinkbase: XmlElement,
    val presentationLinkbase: XmlElement,
    val instanceDocument: XmlElement,
    val schemaExtensionFilename: String,
    val calculationLinkbaseFilename: String,
    val definitionLinkbaseFilename: String,
    val labelLinkbaseFilename: String,
    val presentationLinkbaseFilename: String,
    val instanceDocumentFilename: String,

) {

    val conceptManager: ConceptManager = ConceptManager(this)
    val labelManager: LabelManager = LabelManager(this)
    val factsParser: FactsParser = FactsParser(this)
    val filingArcsParser: FilingArcsParser = FilingArcsParser(this)
    val itemGenerator: ItemGenerator = ItemGenerator(this)

    fun incomeStatementDeclaredDimensions(): List<Dimension> {
        return declaredDimensions(filingArcsParser.parseFilingArcs().incomeStatement)
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
        val instanceDocument = instanceDocument
        val conceptManager = conceptManager

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