package com.starburst.starburst.edgar.factbase.support

import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.dataclasses.Labels
import com.starburst.starburst.edgar.utils.NodeListExtension.attr
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import java.net.URI

class LabelManager(filingProvider: FilingProvider) {

    companion object {
        const val LINK_BASE_NS = "http://www.xbrl.org/2003/linkbase"
    }

    private val labelElement = filingProvider.labelLinkbase()

    private val link = labelElement.getShortNamespace(LINK_BASE_NS)?.let { "$it:" } ?: ""
    private val node = labelElement.getElementByTag("${link}labelLink") ?: error("...")

    // we have loc = locators, label (which is what we want) and labelArc
    // since we are doing a reverse lookup, we need to

    // 1 - build a map of locator(s), from schema definition Ids (the hrefs) -> xlink:label
    private val locs = node
        .getElementsByTag("${link}loc")
        .map {
            val href = it.attr("xlink:href")
            URI(href).fragment to it.attr("xlink:label")
        }.toMap()

    // 2 - build a map of labelArcs, from locator xlink:label -> labelArc
    private val labelArcs = node.getElementsByTag("${link}labelArc").map {
        it.attr("xlink:from") to it.attr("xlink:to")
    }.toMap()

    // 3 - build a map of labelArcs -> label(s)
    private val labels = node.getElementsByTag("${link}label").groupBy {
        it.attr("xlink:label")
    }.map {
            (label, nodes) ->
        // process each node into a map of role -> text
        label to nodes.map {
            it.attr("xlink:role") to it.textContent
        }.toMap()
    }.toMap()

    fun getLabel(schemaElementId: String): Labels {
        val loc = locs[schemaElementId] ?: error("no loc found for $schemaElementId")
        val arcLabel = labelArcs[loc] ?: error("no labelArc found for $loc, derived from $schemaElementId")
        val labels = labels[arcLabel] ?: emptyMap()
        return Labels(
            label = labels["http://www.xbrl.org/2003/role/label"],
            terseLabel = labels["http://www.xbrl.org/2003/role/terseLabel"],
            verboseLabel = labels["http://www.xbrl.org/2003/role/verboseLabel"],
            documentation = labels["http://www.xbrl.org/2003/role/documentation"]
        )
    }

}
