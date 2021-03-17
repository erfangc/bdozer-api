package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.ParseFactsResponse
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.xml.LocalDateExtensions.toLocalDate
import com.starburst.starburst.xml.XmlElement
import com.starburst.starburst.xml.XmlNode
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.time.Instant
import java.time.LocalDate

/**
 * Goes through a single filing as provided by the [FilingProvider]
 * and returns the list of [Fact]s contained within
 */
class FilingParser(private val filingProvider: FilingProvider) {

    companion object {
        const val xbrl = "http://www.xbrl.org/2003/instance"
        const val xbrldi = "http://xbrl.org/2006/xbrldi"
    }

    private val schemaManager = SchemaManager(filingProvider)
    private val labelManager = LabelManager(filingProvider)
    private val instanceDocument = filingProvider.instanceDocument()

    /*
    create a look up of the Xbrl context objects for each fact to refer back to
     */
    private val contexts = instanceDocument
        .getElementsByTag(xbrl, "context")
        .associate { it.attr("id") to toContext(it) }

    private val log = LoggerFactory.getLogger(FilingParser::class.java)

    fun parseFacts(): ParseFactsResponse {

        /*
        go through the instance document and
        reverse lookup everything else
         */
        val instanceDocument = filingProvider.instanceDocument()

        val instanceDocumentFilename = filingProvider.instanceDocumentFilename()
        log.info("Parsing instance document $instanceDocumentFilename")

        /*
        create counters for the different types of facts we will encounter along the way
         */
        var relevant = 0
        var irrelevantTag = 0 // ex: xlink:footnote
        var irrelevantPeriod = 0 // ex: facts with context from past period
        var elementDefinitionNotFound = 0 // ex: other misc problems

        // get some document level information
        val documentFiscalPeriodFocus = documentFiscalPeriodFocus(instanceDocument)
        val documentFiscalYearFocus = documentFiscalYearFocus(instanceDocument)
        val documentPeriodEndDate = documentPeriodEndDate(instanceDocument)

        /*
        now begin processing each element as a potential fact we can create and store
         */
        val facts = instanceDocument.childNodes().mapNotNull { node ->
            val context = getContext(node.attr("contextRef"))
            val conceptDefinition = lookupElementDefinition(node.nodeName)
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
                val formType = formType(instanceDocument)

                Fact(
                    _id = id,
                    instanceDocumentElementId = instanceDocumentElementId,
                    instanceDocumentElementName = node.nodeName,
                    cik = cik,
                    adsh = adsh,
                    entityName = entityName,
                    primarySymbol = primarySymbol,
                    formType = formType,

                    documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.valueOf(documentFiscalPeriodFocus),
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

                    label = labels.label,
                    labelTerse = labels.terseLabel,
                    verboseLabel = labels.verboseLabel,
                    documentation = labels.documentation,

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

        val endDate = documentPeriodEndDate(instanceDocument)

        /*
        figure out the correct period start date
        a canonical must be for this period
         */
        val formType = formType(instanceDocument)
        val startDate = if (formType == "10-K") {
            endDate
                .minusYears(1)
                .plusDays(1)
        } else {
            endDate
                .minusMonths(3)
                .plusDays(1)
        }

        val period = context.period

        return period.instant == endDate
                || (period.startDate == startDate && period.endDate == endDate)

    }

    private fun documentPeriodEndDate(instanceDocument: XmlElement): LocalDate {
        val found = instanceDocument
            .getElementsByTag("dei:DocumentPeriodEndDate")
        if (found.isEmpty()) {
            error("Unable to find DocumentPeriodEndDate for cik=${filingProvider.cik()} adsh=${filingProvider.adsh()}")
        }
        return found
            .first()
            .textContent
            ?.let { date -> LocalDate.parse(date) }
            ?: error("Unable to parse DocumentPeriodEndDate for cik=${filingProvider.cik()} adsh=${filingProvider.adsh()}")
    }

    private fun documentFiscalYearFocus(instanceDocument: XmlElement): Int {
        val found = instanceDocument
            .getElementsByTag("dei:DocumentFiscalYearFocus")
        if (found.isEmpty()) {
            return 0
        }
        return found
            .first()
            .textContent
            .toIntOrNull() ?: 0
    }

    private fun documentFiscalPeriodFocus(instanceDocument: XmlElement): String {
        val found = instanceDocument
            .getElementsByTag("dei:DocumentFiscalPeriodFocus")
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    private fun formType(instanceDocument: XmlElement): String {
        return instanceDocument
            .getElementByTag("dei:DocumentType")
            ?.textContent ?: "Unknown"
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
        return filingProvider.cik()
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

    private fun lookupElementDefinition(nodeName: String): ConceptDefinition? {
        val (namespace, tag) = parseInstanceNodeName(nodeName)
        return if (namespace.isEmpty()) {
            null
        } else {
            schemaManager.getConceptDefinition(namespace, tag)
        }
    }

    private fun getLabels(elementId: String): Labels {
        return labelManager.getLabel(elementId)
    }

    private fun getContext(contextRef: String?): XbrlContext? {
        return contexts[contextRef]
    }

    private fun isNodeRelevant(node: Node): Boolean {
        val (namespace, _) = parseInstanceNodeName(node.nodeName)

        //
        // this node is a fact if it's
        // namespace is one of the ones that matter
        //
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
