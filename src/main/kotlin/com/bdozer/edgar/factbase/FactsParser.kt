package com.bdozer.edgar.factbase

import com.bdozer.edgar.XbrlNamespaces.xbrl
import com.bdozer.edgar.XbrlNamespaces.xbrldi
import com.bdozer.edgar.dataclasses.*
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.edgar.factbase.filing.ContextRelevanceValidator
import com.bdozer.edgar.factbase.filing.SECFiling
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.entityRegistrantName
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.formType
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.tradingSymbol
import com.bdozer.edgar.factbase.ingestor.dataclasses.ParseFactsResponse
import com.bdozer.xml.LocalDateExtensions.toLocalDate
import com.bdozer.xml.XmlNode
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.time.Instant

/**
 * Goes through a single filing as provided by the [secFiling]
 * and returns the list of [Fact]s contained within
 */
class FactsParser(private val secFiling: SECFiling) {

    private val conceptManager = secFiling.conceptManager
    private val labelManager = secFiling.labelManager
    private val instanceDocument = secFiling.instanceDocument
    private val log = LoggerFactory.getLogger(FactsParser::class.java)

    /*
    create a look up of the Xbrl context objects for each fact to refer back to
     */
    val contexts = instanceDocument
        .getElementsByTag(xbrl, "context")
        .associate { it.attr("id") to toContext(it) }

    fun parseFacts(): ParseFactsResponse {

        /*
        go through the instance document and
        reverse lookup everything else
         */
        val instanceDocumentFilename = secFiling.instanceDocumentFilename
        log.info("Parsing instance document $instanceDocumentFilename for facts")

        /*
        create counters for the different types of facts we will encounter along the way
         */
        var relevant = 0
        var irrelevantTag = 0 // ex: xlink:footnote
        var irrelevantContext = 0 // ex: anything outside of the current period
        var elementDefinitionNotFound = 0 // ex: other misc problems

        // get some document level information
        val documentFiscalPeriodFocus = instanceDocument.documentFiscalPeriodFocus()
        val documentFiscalYearFocus = instanceDocument.documentFiscalYearFocus()
        val documentPeriodEndDate = instanceDocument.documentPeriodEndDate() ?: error("...")

        /*
        now begin processing each element as a potential fact we can create and store
         */
        val contextRelevanceValidator = ContextRelevanceValidator(secFiling)
        val facts = instanceDocument
            .childNodes()
            .mapNotNull { node ->
                val context = getContext(node.attr("contextRef"))
                val conceptDefinition = lookupConceptDefinition(node.nodeName)
                val adsh = secFiling.adsh

                /*
                filter the node, if it is not one of the relevant ones, then don't process it
                 */
                if (!isNodeRelevant(node) || context == null) {
                    irrelevantTag++
                    null // return null to skip
                } else if (conceptDefinition == null) {
                    elementDefinitionNotFound++
                    null // return null to skip
                } else if (!contextRelevanceValidator.isContextRelevant(context)) {
                    irrelevantContext++
                    null
                } else {
                    relevant++
                    val content = node.textContent
                    val labels = getLabels(conceptDefinition.id)

                    val explicitMembers = context.entity.segment?.explicitMembers ?: emptyList()
                    val conceptName = conceptDefinition.conceptName

                    /*
                    generate idempotent identifiers if the same information
                    repeats across documents, this way there are no duplicates for the same piece of information
                    */
                    val factIdGenerator = FactIdGenerator()
                    val id = factIdGenerator.generateId(
                        conceptName = conceptName,
                        context = context,
                        documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                    )

                    val instanceDocumentElementId = node.attr("id") ?: "N/A"
                    val cik = secFiling.cik
                    val formType = instanceDocument.formType()

                    /*
                    Create the Fact instance we just parsed
                     */
                    Fact(
                        _id = id,
                        instanceDocumentElementId = instanceDocumentElementId,
                        instanceDocumentElementName = node.nodeName,
                        cik = cik,
                        adsh = adsh,
                        entityName = instanceDocument.entityRegistrantName(),
                        primarySymbol = instanceDocument.tradingSymbol(),
                        formType = formType,

                        documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                        documentFiscalYearFocus = documentFiscalYearFocus,
                        documentPeriodEndDate = documentPeriodEndDate,

                        conceptName = conceptName,
                        conceptHref = conceptDefinition.conceptHref,
                        namespace = conceptDefinition.targetNamespace,

                        startDate = context.period.startDate,
                        endDate = context.period.endDate,
                        instant = context.period.instant,
                        explicitMembers = explicitMembers,

                        sourceDocument = sourceDocument(node),

                        label = labels?.label,
                        labelTerse = labels?.terseLabel,
                        verboseLabel = labels?.verboseLabel,
                        documentation = labels?.documentation,

                        stringValue = content,
                        doubleValue = content.toDoubleOrNull(),

                        lastUpdated = Instant.now().toString(),
                    )
                }
            }

        /*
        the XMLs sometimes contain duplicates
         */
        val distinctFacts = facts.distinctBy { it._id }
        log.info(
            "$instanceDocumentFilename has ${distinctFacts.size} distinct relevant facts, " +
                    "totalRelevantFacts=${facts.size}, " +
                    "relevant=$relevant, " +
                    "irrelevantTag=$irrelevantTag, " +
                    "irrelevantContext=$irrelevantContext, " +
                    "elementDefinitionNotFound=$elementDefinitionNotFound"
        )

        return ParseFactsResponse(
            facts = distinctFacts,
            documentFiscalPeriodFocus = documentFiscalPeriodFocus,
            documentPeriodEndDate = documentPeriodEndDate,
            documentFiscalYearFocus = documentFiscalYearFocus,
        )

    }

    private fun sourceDocument(node: XmlNode): String {
        val cik = secFiling.cik
        val adsh = secFiling.adsh.replace("-", "")
        val fileName = secFiling.inlineHtml
        val id = node.attr("id")
        return if (id == null) {
            "https://www.sec.gov/ix?doc=/Archives/edgar/data/$cik/$adsh/$fileName"
        } else {
            "https://www.sec.gov/ix?doc=/Archives/edgar/data/$cik/$adsh/$fileName#$id"
        }
    }

    private fun parseInstanceNodeName(nodeName: String?): Pair<String, String> {
        if (nodeName == null)
            error("nodeName cannot be null")
        val instanceDocument = secFiling.instanceDocument
        val parts = nodeName.split(":".toRegex(), 2)
        return if (parts.size == 1) {
            Pair(instanceDocument.defaultLongNamespace() ?: "", nodeName)
        } else {
            val shortNamespace = parts.first()
            val first = instanceDocument.shortNamespaceToLongNamespaceMap()[shortNamespace] ?: ""
            Pair(first, parts.last())
        }
    }

    private fun lookupConceptDefinition(nodeName: String): Concept? {
        val (namespace, tag) = parseInstanceNodeName(nodeName)
        return if (namespace.isEmpty()) {
            null
        } else {
            conceptManager.getConcept(namespace, tag)
        }
    }

    private fun getLabels(elementId: String): Labels? {
        return labelManager.getLabel(elementId)
    }

    private fun getContext(contextRef: String?): XbrlContext? {
        return contexts[contextRef]
    }

    private fun isNodeRelevant(node: Node): Boolean {
        val (namespace, _) = parseInstanceNodeName(node.nodeName)
        /*
        this node is a fact if it's
        namespace is one of the ones that matter
         */
        val isExternalNamespace = namespace == secFiling.schema.targetNamespace()
        val lookup = instanceDocument.longNamespaceToShortNamespaceMap()
        return isRelevantElementDefinition(isExternalNamespace, lookup, namespace)
    }

    private fun isRelevantElementDefinition(
        isExternalNamespace: Boolean,
        lookup: Map<String, String>,
        namespace: String
    ) = (isExternalNamespace
            || lookup[namespace] == "us-gaap"
            || lookup[namespace] == "dei")

    private fun toContext(node: XmlNode): XbrlContext {

        val period = node.getElementByTag(xbrl, "period")
        val entity = node.getElementByTag(xbrl, "entity")
        val identifier = entity?.getElementByTag(xbrl, "identifier")

        val explicitMembers = entity
            ?.getElementByTag(xbrl, "segment")
            ?.getElementsByTag(xbrldi, "explicitMember")
            ?.map { myNode ->
                XbrlExplicitMember(
                    dimension = myNode.attributes.getNamedItem("dimension").textContent,
                    value = myNode.textContent
                )
            }

        return XbrlContext(
            id = node.attributes.getNamedItem("id").textContent,
            entity = XbrlEntity(
                identifier = XbrlIdentifier(
                    scheme = identifier?.attributes?.getNamedItem("scheme")?.textContent,
                    value = identifier?.textContent
                ),
                segment = explicitMembers?.let {
                    XbrlSegment(
                        explicitMembers = explicitMembers
                    )
                }
            ),
            period = XbrlPeriod(
                instant = period?.getElementByTag(xbrl, "instant")?.toLocalDate(),
                startDate = period?.getElementByTag(xbrl, "startDate")?.toLocalDate(),
                endDate = period?.getElementByTag(xbrl, "endDate")?.toLocalDate(),
            )
        )

    }

}
