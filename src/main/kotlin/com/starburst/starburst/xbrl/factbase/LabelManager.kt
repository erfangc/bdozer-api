package com.starburst.starburst.xbrl.factbase

import com.starburst.starburst.xbrl.FilingProvider
import com.starburst.starburst.xbrl.dataclasses.Labels
import com.starburst.starburst.xbrl.utils.ElementExtension.getDefaultLongNamespace
import com.starburst.starburst.xbrl.utils.ElementExtension.longNamespaceToShortNamespaceMap
import com.starburst.starburst.xbrl.utils.NodeListExtension.attr
import com.starburst.starburst.xbrl.utils.NodeListExtension.findAllByTag
import com.starburst.starburst.xbrl.utils.NodeListExtension.findByTag
import java.net.URI

class LabelManager(filingProvider: FilingProvider) {

    private val labelElement = filingProvider.labelLinkbase()
    private val targetNamespace = "http://www.xbrl.org/2003/linkbase"

    private val nsPrefix = if (labelElement.getDefaultLongNamespace() == targetNamespace)
        ""
    else {
        val ns = labelElement
            .longNamespaceToShortNamespaceMap()
            .entries
            .find { it.key == targetNamespace }
            ?.value ?: error("...")
        "$ns:"
    }

    private val node = labelElement.findByTag("${nsPrefix}labelLink") ?: error("...")

    // we have loc = locators, label (which is what we want) and labelArc
    // since we are doing a reverse lookup, we need to

    // 1 - build a map of locator(s), from schema definition Ids (the hrefs) -> xlink:label
    private val locs = node
        .findAllByTag("${nsPrefix}loc")
        .map {
            val href = it.attr("xlink:href")
            URI(href).fragment to it.attr("xlink:label")
        }.toMap()

    // 2 - build a map of labelArcs, from locator xlink:label -> labelArc
    private val labelArcs = node.findAllByTag("${nsPrefix}labelArc").map {
        it.attr("xlink:from") to it.attr("xlink:to")
    }.toMap()

    // 3 - build a map of labelArcs -> label(s)
    private val labels = node.findAllByTag("${nsPrefix}label").groupBy {
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
