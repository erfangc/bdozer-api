package com.starburst.starburst.edgar.factbase.ingestor.support

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.XbrlNamespaces.xbrl
import com.starburst.starburst.edgar.XbrlNamespaces.xbrldi
import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentFiscalYearFocus
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.starburst.starburst.edgar.factbase.ingestor.InstanceDocumentExtensions.formType
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.ParseFactsResponse
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.xml.LocalDateExtensions.toLocalDate
import com.starburst.starburst.xml.XmlElement
import com.starburst.starburst.xml.XmlNode
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

/**
 * Goes through a single filing as provided by the [FilingProvider]
 * and returns the list of [Fact]s contained within
 */
class FactsParser(private val filingProvider: FilingProvider) {

    private val schemaManager = SchemaManager(filingProvider)
    private val labelManager = LabelManager(filingProvider)
    private val instanceDocument = filingProvider.instanceDocument()
    private val log = LoggerFactory.getLogger(FactsParser::class.java)

    /*
    create a look up of the Xbrl context objects for each fact to refer back to
     */
    private val contexts = instanceDocument
        .getElementsByTag(xbrl, "context")
        .associate { it.attr("id") to toContext(it) }

    fun parseFacts(): ParseFactsResponse {

        /*
        go through the instance document and
        reverse lookup everything else
         */
        val instanceDocumentFilename = filingProvider.instanceDocumentFilename()
        log.info("Parsing instance document $instanceDocumentFilename for facts")

        /*
        create counters for the different types of facts we will encounter along the way
         */
        var relevant = 0
        var irrelevantTag = 0 // ex: xlink:footnote
        var irrelevantPeriod = 0 // ex: facts with context from past period
        var elementDefinitionNotFound = 0 // ex: other misc problems

        // get some document level information
        val documentFiscalPeriodFocus = instanceDocument.documentFiscalPeriodFocus()
        val documentFiscalYearFocus = instanceDocument.documentFiscalYearFocus()
        val documentPeriodEndDate = instanceDocument.documentPeriodEndDate() ?: error("...")

        /*
        now begin processing each element as a potential fact we can create and store
         */
        val facts = instanceDocument.childNodes().mapNotNull { node ->
            val context = getContext(node.attr("contextRef"))
            val conceptDefinition = lookupConceptDefinition(node.nodeName)
            val adsh = filingProvider.adsh()

            /*
            filter the node, if it is not one of the relevant ones, then don't process it
             */
            if (!isNodeRelevant(node) || context == null) {
                irrelevantTag++
                null // return null to skip
            } else if (!isFactInPeriod(context)) {
                irrelevantPeriod++
                null // return null to skip
            } else if (conceptDefinition == null) {
                elementDefinitionNotFound++
                null // return null to skip
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
                    elementName = conceptName,
                    context = context,
                    documentPeriodEndDate = documentPeriodEndDate
                )

                val instanceDocumentElementId = node.attr("id") ?: "N/A"
                val cik = cik()
                val entityName = entityRegistrantName(instanceDocument)
                val primarySymbol = primarySymbol(instanceDocument)
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
                    entityName = entityName,
                    primarySymbol = primarySymbol,
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
        // the XMLs sometimes contain duplicates
        val distinctFacts = facts.distinctBy { it._id }
        log.info(
            "$instanceDocumentFilename has ${distinctFacts.size} distinct relevant facts, " +
                    "totalRelevantFacts=${facts.size}, " +
                    "relevant=$relevant, " +
                    "irrelevantTag=$irrelevantTag, " +
                    "irrelevantPeriod=$irrelevantPeriod, " +
                    "elementDefinitionNotFound=$elementDefinitionNotFound"
        )

        return ParseFactsResponse(
            facts = distinctFacts,
            documentFiscalPeriodFocus = documentFiscalPeriodFocus,
            documentPeriodEndDate = documentPeriodEndDate,
            documentFiscalYearFocus = documentFiscalYearFocus
        )

    }

    /**
     * Determines if the fact being reported is for the period
     * it is intended to be reported and not a restatement of some historical period
     */
    private fun isFactInPeriod(context: XbrlContext): Boolean {

        val endDate = instanceDocument.documentPeriodEndDate() ?: error("...")

        /*
        figure out the correct period start date
        a canonical must be for this period
         */
        val formType = instanceDocument.formType()
        val startDate = if (formType == "10-K") {
            endDate
                .minusYears(1)
                .plusDays(1)
        } else {
            endDate
                .minusMonths(3)
                .with(lastDayOfMonth())
                .plusDays(1)
        }

        val period = context.period

        return period.instant == endDate
                || (period.startDate == startDate && period.endDate == endDate)

    }

    private fun sourceDocument(node: XmlNode): String {
        val cik = filingProvider.cik()
        val adsh = filingProvider.adsh().replace("-", "")
        val fileName = filingProvider.inlineHtml()
        val id = node.attr("id")
        return if (id == null) {
            "https://www.sec.gov/ix?doc=/Archives/edgar/data/$cik/$adsh/$fileName"
        } else {
            "https://www.sec.gov/ix?doc=/Archives/edgar/data/$cik/$adsh/$fileName#$id"
        }
    }

    private fun primarySymbol(instanceDocument: XmlElement): String {
        val found = instanceDocument
            .getElementsByTag("dei:TradingSymbol")

        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    private fun entityRegistrantName(instanceDocument: XmlElement): String {
        val found = instanceDocument
            .getElementsByTag("dei:EntityRegistrantName")
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    private fun cik(): String {
        return filingProvider.cik().padStart(10, '0')
    }

    private fun parseInstanceNodeName(nodeName: String?): Pair<String, String> {
        if (nodeName == null)
            error("nodeName cannot be null")
        val instanceDocument = filingProvider.instanceDocument()
        val parts = nodeName.split(":".toRegex(), 2)
        return if (parts.size == 1) {
            Pair(instanceDocument.defaultLongNamespace() ?: "", nodeName)
        } else {
            val shortNamespace = parts.first()
            val first = instanceDocument.shortNamespaceToLongNamespaceMap()[shortNamespace] ?: ""
            Pair(first, parts.last())
        }
    }

    private fun lookupConceptDefinition(nodeName: String): ConceptDefinition? {
        val (namespace, tag) = parseInstanceNodeName(nodeName)
        return if (namespace.isEmpty()) {
            null
        } else {
            schemaManager.getConceptDefinition(namespace, tag)
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
        val isExternalNamespace = namespace == filingProvider.schema().targetNamespace()
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
