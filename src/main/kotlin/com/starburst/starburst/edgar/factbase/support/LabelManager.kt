package com.starburst.starburst.edgar.factbase.support

import com.starburst.starburst.edgar.dataclasses.Labels
import com.starburst.starburst.edgar.provider.FilingProvider
import java.net.URI

/**
 * Traverses the [FilingProvider.labelLinkbase] XML document for labels
 * given a element id from the instance file
 */
class LabelManager(filingProvider: FilingProvider) {

    companion object {
        const val LINK_BASE_NS = "http://www.xbrl.org/2003/linkbase"
    }

    private val labelElement = filingProvider.labelLinkbase()

    private val linkNs = labelElement.getShortNamespace(LINK_BASE_NS)

    private val labelLink = linkNs?.let { "$it:labelLink" } ?: "labelLink"
    private val labelArc = linkNs?.let { "$it:labelArc" } ?: "labelArc"
    private val label = linkNs?.let { "$it:label" } ?: "label"
    private val loc = linkNs?.let { "$it:loc" } ?: "loc"

    private val node = labelElement.getElementByTag(labelLink) ?: error("...")

    //
    // we have loc = locators, label (which is what we want) and labelArc
    // since we are doing a reverse lookup, we need to
    //

    //
    // 1 - build a map of locator(s), from schema definition Ids (the hrefs) -> xlink:label
    //
    private val locs = node
        .getElementsByTag(loc)
        .map {
            URI(it.attr("xlink:href")).fragment to it.attr("xlink:label")
        }.toMap()

    //
    // 2 - build a map of labelArcs, from locator xlink:label -> labelArc
    //
    private val labelArcs = node
        .getElementsByTag(labelArc)
        .map {
            it.attr("xlink:from") to it.attr("xlink:to")
        }
        .toMap()

    //
    // 3 - build a map of labelArcs -> label(s)
    //
    private val labels = node
        .getElementsByTag(label)
        .groupBy {
            it.attr("xlink:label")
        }.map { (label, nodes) ->
            // process each node into a map of role -> text
            label to nodes.map {
                it.attr("xlink:role") to it.textContent
            }.toMap()
        }.toMap()

    fun getLabel(schemaElementId: String): Labels {
        val loc = locs[schemaElementId] ?: error("no loc found for $schemaElementId")
        val arcLabel = labelArcs[loc] ?: error("no labelArc found for $loc, derived from $schemaElementId")
        val labels = labels[arcLabel] ?: emptyMap()

        val label = labels["http://www.xbrl.org/2003/role/label"]
        val terseLabel = labels["http://www.xbrl.org/2003/role/terseLabel"] ?: label
        val verboseLabel = labels["http://www.xbrl.org/2003/role/verboseLabel"] ?: terseLabel
        val documentation = labels["http://www.xbrl.org/2003/role/documentation"]
        return Labels(
            label = label,
            terseLabel = terseLabel,
            verboseLabel = verboseLabel,
            documentation = documentation
        )
    }

}
