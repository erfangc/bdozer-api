package com.starburst.starburst.xbrl.factbase

import com.starburst.starburst.xbrl.FilingProvider
import com.starburst.starburst.xbrl.dataclasses.*
import com.starburst.starburst.xbrl.utils.ElementExtension.getDefaultLongNamespace
import com.starburst.starburst.xbrl.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.xbrl.utils.ElementExtension.longNamespaceToShortNamespaceMap
import com.starburst.starburst.xbrl.utils.ElementExtension.shortNamespaceToLongNamespaceMap
import com.starburst.starburst.xbrl.utils.ElementExtension.targetNamespace
import com.starburst.starburst.xbrl.utils.LocalDateExtensions.toLocalDate
import com.starburst.starburst.xbrl.utils.NodeListExtension.attr
import com.starburst.starburst.xbrl.utils.NodeListExtension.findAllByTag
import com.starburst.starburst.xbrl.utils.NodeListExtension.findByTag
import com.starburst.starburst.xbrl.utils.NodeListExtension.map
import com.starburst.starburst.xbrl.utils.NodeListExtension.toList
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.time.Instant

class XbrlFactParser(private val filingProvider: FilingProvider) {

    private val schemaManager = SchemaManager(filingProvider)
    private val labelManager = LabelManager(filingProvider)
    private val instanceDocument = filingProvider.instanceDocument()
    private val contexts = instanceDocument
        .findAllByTag("context")
        .associate { it.attr("id") to toContext(it) }

    fun parseFacts(): List<Fact> {

        /*
        go through the instance document and
        reverse lookup everything else
         */
        val instanceDocument = filingProvider.instanceDocument()


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
                val content = node.textContent
                val labels = elementDefinition?.let { getLabels(it.id) }
                val xbrlContext = getContext(node.attr("contextRef"))

                Fact(
                    _id = factIdGenerator.generateId(node, xbrlContext),
                    cik = cik(instanceDocument),
                    nodeName = node.nodeName,
                    entityName = entityRegistrantName(instanceDocument),
                    primarySymbol = primarySymbol(instanceDocument),
                    symbols = symbols(instanceDocument),
                    period = xbrlContext.period,
                    explicitMembers = xbrlContext.entity.segment?.explicitMembers ?: emptyList(),
                    sourceDocument = sourceDocument(node),
                    label = labels?.label,
                    labelTerse = labels?.terseLabel,
                    verboseLabel = labels?.verboseLabel,
                    stringValue = content,
                    doubleValue = content.toDoubleOrNull(),
                    lastUpdated = Instant.now().toString(),
                    formType = "10-K" // TODO
                )
            } else {
                null
            }
        }
        return facts.filterNotNull()
    }

    private fun sourceDocument(node: Node): String {
        val cik = filingProvider.cik()
        val adsh = filingProvider.adsh()
        val fileName = filingProvider.instanceDocumentFilename()
        val id = node.attr("id")
        return if (id == null) {
            "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}/$fileName"
        } else {
            "https://www.sec.gov/Archives/edgar/data/$cik/${adsh.replace("-", "")}/$fileName#$id"
        }
    }

    private fun symbols(instanceDocument: Element): List<String> {
        return listOf(primarySymbol(instanceDocument))
    }

    private fun primarySymbol(instanceDocument: Element): String {
        val found = instanceDocument
            .getElementsByTagNameSafe("dei:TradingSymbol")
            .toList()
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    private fun entityRegistrantName(instanceDocument: Element): String {
        val found = instanceDocument
            .getElementsByTagNameSafe("dei:EntityRegistrantName")
            .toList()
        if (found.isEmpty()) {
            return "N/A"
        }
        return found
            .first()
            .textContent ?: "N/A"
    }

    private fun cik(instanceDocument: Element): String {
        return filingProvider.cik()
    }

    private fun parseInstanceNodeName(nodeName: String?): Pair<String, String> {
        if (nodeName == null)
            error("...")
        val instanceDocument = filingProvider.instanceDocument()
        val parts = nodeName.split(":".toRegex(), 2)
        return if (parts.size == 1) {
            Pair(instanceDocument.getDefaultLongNamespace() ?: "", nodeName)
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
        val isExt = namespace == filingProvider.schemaExtension().targetNamespace()
        val lookup = instanceDocument.longNamespaceToShortNamespaceMap()
        return isExt || lookup[namespace] == "us-gaap" || lookup[namespace] == "dei"
    }

    private fun toContext(node: Node): XbrlContext {
        if (node.nodeName != "context")
            error("nodeNode must be context")

        val period = node.findByTag("period")
        val entity = node.findByTag("entity")
        val identifier = entity?.findByTag("identifier")

        val explicitMembers = entity
            ?.findByTag("segment")
            ?.findAllByTag("xbrldi:explicitMember")
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
                instant = period?.findByTag("instant")?.toLocalDate(),
                startDate = period?.findByTag("startDate")?.toLocalDate(),
                endDate = period?.findByTag("endDate")?.toLocalDate(),
            )
        )

    }

}