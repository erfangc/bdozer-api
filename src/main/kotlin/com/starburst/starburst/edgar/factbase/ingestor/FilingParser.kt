package com.starburst.starburst.edgar.factbase.ingestor

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.utils.LocalDateExtensions.toLocalDate
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.map
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.time.Instant

/**
 * Goes through a single filing as provided by the [FilingProvider]
 * and returns the list of [Fact]s contained within
 */
class FilingParser(private val filingProvider: FilingProvider) {

    private val schemaManager = SchemaManager(filingProvider)
    private val labelManager = LabelManager(filingProvider)
    private val instanceDocument = filingProvider.instanceDocument()
    private val contexts = instanceDocument
        .getElementsByTag("context")
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

                if (elementDefinition != null) {
                    val content = node.textContent
                    val labels = getLabels(elementDefinition.id)
                    val xbrlContext = getContext(node.attr("contextRef"))

                    Fact(
                        _id = factIdGenerator.generateId(node, xbrlContext),
                        instanceDocumentElementId = node.attr("id") ?: "N/A",
                        cik = cik(instanceDocument),
                        entityName = entityRegistrantName(instanceDocument),
                        primarySymbol = primarySymbol(instanceDocument),
                        symbols = symbols(instanceDocument),
                        formType = formType(instanceDocument),

                        elementName = elementDefinition.name,
                        rawElementName = node.nodeName,
                        longNamespace = elementDefinition.longNamespace,

                        period = xbrlContext.period,
                        explicitMembers = xbrlContext.entity.segment?.explicitMembers ?: emptyList(),

                        sourceDocument = sourceDocument(node),

                        label = labels.label,
                        labelTerse = labels.terseLabel,
                        verboseLabel = labels.verboseLabel,

                        stringValue = content,
                        doubleValue = content.toDoubleOrNull(),

                        lastUpdated = Instant.now().toString(),
                    )
                } else {
                    log.error("Unable to find an element definition for ${node.nodeName} cik=${filingProvider.cik()} adsh=${filingProvider.adsh()}")
                    null
                }
            } else {
                // skipping the node b/c it is irrelevant
                null
            }
        }
        // TODO deduplicate the fact(s)
        val ret = facts.filterNotNull()
        log.info("Found ${ret.size} facts in $instanceDocumentFilename, out of ${facts.size} expected, after removing duplicates")
        return ret
    }

    private fun formType(instanceDocument: XmlElement): String {
        return instanceDocument
            .getElementByTag("dei:DocumentType")
            ?.textContent ?: "Unknown"
    }

    private fun sourceDocument(node: Node): String {
        val cik = filingProvider.cik()
        val adsh = filingProvider.adsh()
        val fileName = filingProvider.inlineHtml()
        val id = node.attr("id")
        return if (id == null) {
            "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}/$fileName"
        } else {
            "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}/$fileName#$id"
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
        if (node.nodeName != "context")
            error("nodeNode must be context")

        val period = node.getElementByTag("period")
        val entity = node.getElementByTag("entity")
        val identifier = entity?.getElementByTag("identifier")

        val explicitMembers = entity
            ?.getElementByTag("segment")
            ?.getElementsByTag("xbrldi:explicitMember")
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
