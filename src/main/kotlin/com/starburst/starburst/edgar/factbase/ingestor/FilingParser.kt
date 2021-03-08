package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.utils.LocalDateExtensions.toLocalDate
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.map
import org.slf4j.LoggerFactory
import org.threeten.extra.YearQuarter
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
    private val contexts = instanceDocument
        .getElementsByTag(xbrl, "context")
        .associate { it.attr("id") to toContext(it) }

    private val log = LoggerFactory.getLogger(FilingParser::class.java)

    fun parseFacts(): List<Fact> {

        /*
        go through the instance document and
        reverse lookup everything else
         */
        val instanceDocument = filingProvider.instanceDocument()

        val instanceDocumentFilename = filingProvider.instanceDocumentFilename()
        log.info("Parsing instance document $instanceDocumentFilename")

        /*
        parse the context first - create the lookup map
        for context(s)
         */

        /*
        parse the label(s) next - create the lookup map
         */

        val facts = instanceDocument.childNodes.map { node ->
            //
            // filter the node, if it is not one of the relevant ones, then don't process it
            //
            if (isRelevant(node)) {
                /*
                generate idempotent identifiers if the same information
                repeats across documents, this way there are no duplicates for the same piece of information
                 */
                val factIdGenerator = FactIdGenerator(instanceDocument)

                /*
                assemble the [Fact] from the relevant context and label links
                 */
                val elementDefinition = lookupElementDefinition(node.nodeName)

                val adsh = filingProvider.adsh()
                if (elementDefinition != null) {
                    val content = node.textContent
                    val labels = getLabels(elementDefinition.id)
                    val xbrlContext = getContext(node.attr("contextRef"))

                    val symbols = symbols(instanceDocument)
                    val documentFiscalPeriodFocus = documentFiscalPeriodFocus(instanceDocument)
                    val documentFiscalYearFocus = documentFiscalYearFocus(instanceDocument)
                    val documentPeriodEndDate = documentPeriodEndDate(instanceDocument)
                    val explicitMembers = xbrlContext.entity.segment?.explicitMembers ?: emptyList()

                    val _id = factIdGenerator.generateId(
                        node = node,
                        context = xbrlContext,
                        documentPeriodEndDate = documentPeriodEndDate
                    )

                    val instanceDocumentElementId = node.attr("id") ?: "N/A"
                    val cik = cik(instanceDocument)
                    val entityName = entityRegistrantName(instanceDocument)
                    val primarySymbol = primarySymbol(instanceDocument)
                    val formType = formType(instanceDocument)


                    Fact(
                        _id = _id,
                        instanceDocumentElementId = instanceDocumentElementId,
                        cik = cik,
                        adsh = adsh,
                        entityName = entityName,
                        primarySymbol = primarySymbol,
                        symbols = symbols,
                        formType = formType,

                        canonical = isCanonical(instanceDocument, xbrlContext),

                        documentFiscalPeriodFocus = documentFiscalPeriodFocus,
                        documentFiscalYearFocus = documentFiscalYearFocus,
                        documentPeriodEndDate = documentPeriodEndDate,

                        elementName = elementDefinition.name,
                        rawElementName = node.nodeName,
                        longNamespace = elementDefinition.longNamespace,

                        period = xbrlContext.period,
                        explicitMembers = explicitMembers,

                        sourceDocument = sourceDocument(node),

                        label = labels.label,
                        labelTerse = labels.terseLabel,
                        verboseLabel = labels.verboseLabel,

                        stringValue = content,
                        doubleValue = content.toDoubleOrNull(),

                        lastUpdated = Instant.now().toString(),
                    )
                } else {
                    log.error("Unable to find an element definition for ${node.nodeName} cik=${filingProvider.cik()} adsh=$adsh")
                    null
                }
            } else {
                // skipping the node b/c it is irrelevant
                null
            }
        }
        val ret = facts.filterNotNull()
        log.info("Found ${ret.size} facts in $instanceDocumentFilename, out of ${facts.size} expected, after removing duplicates")
        return ret
    }

    private fun isCanonical(
        instanceDocument: XmlElement,
        context: XbrlContext
    ): Boolean {

        val endDate = LocalDate.parse(documentPeriodEndDate(instanceDocument))

        /*
        Figure out the correct period start date
        a canonical must be for this period
         */
        val formType = formType(instanceDocument)
        val startDate = if (formType == "10-K") {
            endDate
                .minusYears(1)
                .plusDays(1)
        } else {
            YearQuarter
                .from(endDate)
                .minusQuarters(1)
                .atEndOfQuarter()
                .plusDays(1)
        }

        val entity = context.entity
        val period = context.period

        return (period.instant == endDate
                || (period.startDate == startDate && period.endDate == endDate))
                && (entity.segment?.explicitMembers?.isNullOrEmpty() ?: true)
    }

    private fun documentPeriodEndDate(instanceDocument: XmlElement): String {
        val found = instanceDocument
            .getElementsByTag("dei:DocumentPeriodEndDate")
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
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

    private fun sourceDocument(node: Node): String {
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

    private fun symbols(instanceDocument: XmlElement): List<String> {
        return listOf(primarySymbol(instanceDocument))
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

    private fun cik(instanceDocument: XmlElement): String {
        return filingProvider.cik()
    }

    private fun parseInstanceNodeName(nodeName: String?): Pair<String, String> {
        if (nodeName == null)
            error("...")
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

    private fun lookupElementDefinition(nodeName: String): ElementDefinition? {
        val (namespace, tag) = parseInstanceNodeName(nodeName)
        return schemaManager.getElementDefinition(namespace, tag)
    }

    private fun getLabels(elementId: String): Labels {
        return labelManager.getLabel(elementId)
    }

    private fun getContext(contextRef: String?): XbrlContext {
        return contexts[contextRef] ?: error("unable to find context $contextRef")
    }

    private fun isRelevant(node: Node): Boolean {
        val (namespace, _) = parseInstanceNodeName(node.nodeName)
        // this node is a fact if it's namespace is one of the ones that matter
        val isExt = namespace == filingProvider.schema().targetNamespace()
        val lookup = instanceDocument.longNamespaceToShortNamespaceMap()
        return isExt || lookup[namespace] == "us-gaap" || lookup[namespace] == "dei"
    }

    private fun toContext(node: Node): XbrlContext {

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
                instant = period?.getElementByTag("instant")?.toLocalDate(),
                startDate = period?.getElementByTag("startDate")?.toLocalDate(),
                endDate = period?.getElementByTag("endDate")?.toLocalDate(),
            )
        )

    }

}
