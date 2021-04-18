package com.bdozer.edgar.factbase.filing

import com.bdozer.edgar.XbrlNamespaces.link
import com.bdozer.edgar.XbrlNamespaces.xlink
import com.bdozer.edgar.dataclasses.Labels
import java.net.URI

/**
 * Traverses the [SECFiling.labelLinkbase] XML document for labels
 * given a element id from the instance file
 */
class LabelManager(secFiling: SECFiling) {

    private val labelElement = secFiling.labelLinkbase
    private val node = labelElement.getElementByTag(link, "labelLink")
        ?: error("${secFiling.labelLinkbaseFilename} does not contain labelLink")

    /*
    we have loc = locators, label (which is what we want) and labelArc
    since we are doing a reverse lookup, we need to
     */

    /*
    1 - build a map of locator(s), from schema definition Ids (the hrefs) -> xlink:label
     */
    private val locs = node
        .getElementsByTag(link, "loc")
        .associate {
            val href = it.attr(xlink, "href") ?: error("href not defined on label locator")
            val label = it.attr(xlink, "label")
            URI(href).fragment to label
        }

    /*
    2 - build a map of labelArcs, from locator xlink:label -> labelArc
     */
    private val labelArcs = node
        .getElementsByTag(link, "labelArc")
        .associate {
            it.attr(xlink, "from") to it.attr(xlink, "to")
        }

    /*
    3 - build a map of labelArcs -> label(s)
     */
    private val labels = node
        .getElementsByTag(link, "label")
        .groupBy { it.attr(xlink, "label") }
        .map { (label, nodes) ->
            /*
            process each node into a map of role -> text
             */
            label to nodes.associate {
                it.attr(xlink, "role") to it.textContent
            }
        }
        .toMap()

    fun getLabel(conceptId: String): Labels? {
        val loc = locs[conceptId] ?: return null
        val arcLabel = labelArcs[loc] ?: return null
        val labels = labels[arcLabel] ?: emptyMap()

        val label = labels["http://www.xbrl.org/2003/role/label"]
        val terseLabel = labels["http://www.xbrl.org/2003/role/terseLabel"]
        val verboseLabel = labels["http://www.xbrl.org/2003/role/verboseLabel"]
        val documentation = labels["http://www.xbrl.org/2003/role/documentation"]

        return Labels(
            label = label,
            terseLabel = terseLabel,
            verboseLabel = verboseLabel,
            documentation = documentation,
        )
    }

}
