package co.bdozer.libraries.tenk.sectionparser

import org.apache.commons.text.similarity.CosineDistance
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayDeque

class TenKSectionExtractor {

    private val log = LoggerFactory.getLogger(TenKSectionExtractor::class.java)
    private val distance = CosineDistance()

    /**
     * Take a 10-K document and find the
     * Item 1, 1A sections DOM elements from it
     */
    fun extractSections(doc: Document): TenKSections? {
        val item1 = findAnchor(document = doc, query = "item 1. business")
        val item1a = findAnchor(document = doc, query = "item 1a. risk factors")
        val item1b = findAnchor(document = doc, query = "item 1b. unresolved staff comments")
        
        if (item1 == null || item1a == null) {
            return null
        }
        
        val businessSection =
            TenKSection(name = item1.text, startAnchor = item1.anchor, endAnchor = item1a.anchor)
        val riskFactorsSection =
            TenKSection(name = item1a.text, startAnchor = item1a.anchor, endAnchor = item1b?.anchor)
        
        return TenKSections(
            business = businessSection.copy(elements = findElements(doc, businessSection)),
            riskFactors = riskFactorsSection.copy(elements = findElements(doc, riskFactorsSection)),
        )
    }
    
    private fun findAnchor(document: Document, query: String): Candidate? {
        val tables = document.select("table").take(50)
        val candidate = tables
            .flatMap { table -> candidates(table, query) }
            .sortedBy { it.score }
            .firstOrNull {
                it.anchor != null
            }
        return candidate
    }
    
    /**
     * Find the corresponding DOM elements that corresponds to a given section
     */
    private fun findElements(
        doc: Document,
        tenKSection: TenKSection,
    ): List<Element> {
        val startAnchor = tenKSection.startAnchor ?: return emptyList()
        val endAnchor = tenKSection.endAnchor ?: return emptyList()

        val start = doc.getElementById(startAnchor.replaceFirst("#", "")) ?: return emptyList()

        val queue = ArrayDeque<Element>()
        queue.add(start)

        val ret = arrayListOf<Element>()

        var endAnchorFound = false
        while (!endAnchorFound && queue.isNotEmpty()) {
            val element = queue.removeFirst()
            ret.add(element)
            if (containsEndAnchor(endAnchor, element)) {
                endAnchorFound = true
            } else {
                if (queue.isEmpty()) {
                    queue.addAll(ancestorSiblings(element))
                }
            }
        }
        removeTables(ret)
        return ret
    }

    private fun ancestorSiblings(element: Element): List<Element> {
        var current: Element? = element
        while (current?.parent() != null && current.nextElementSiblings().isEmpty()) {
            current = current.parent()
        }
        return current?.nextElementSiblings() ?: emptyList()
    }


    private fun removeTables(elements: List<Element>) {
        elements.forEach { removeTables(it) }
    }

    /**
     * Perform a DFT on the element and it's children
     * when we see a <table></table> be sure to get rid of it
     * from it DOM
     */
    private fun removeTables(element: Element) {
        val stack = Stack<Element>()
        stack.addAll(element.children())
        while (stack.isNotEmpty()) {
            val elm = stack.pop()
            if (elm.tagName() == "table") {
                elm.remove()
            } else {
                stack.addAll(elm.children())
            }
        }
    }

    private fun containsEndAnchor(endAnchor: String, root: Element): Boolean {
        if (root.id() == endAnchor.replaceFirst("#", "")) {
            return true
        }

        val stack = Stack<Element>()
        stack.addAll(root.children())

        while (stack.isNotEmpty()) {
            val element = stack.pop()
            if (element.id() == endAnchor.replaceFirst("#", "")) {
                return true
            }
            stack.addAll(element.children())
        }
        return false
    }

    private fun candidates(table: Element, query: String): List<Candidate> {
        val rows = table.select("tbody > tr")
        return rows.mapNotNull { row ->
            val cells = row.select("td")
            val text = cells.joinToString(" ") { cell ->
                cell.text().trim()
            }
            if (text.trim().isNotEmpty()) {
                val score = distance.apply(query, text.lowercase().trim())
                val anchors = cells.select("a")
                Candidate(
                    text = text,
                    anchor = anchors.firstOrNull()?.attr("href")?.trim(),
                    score = score
                )
            } else {
                null
            }
        }
    }
}
