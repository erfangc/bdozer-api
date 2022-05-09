package co.bdozer.libraries.tenk.sectionparser

import co.bdozer.core.nlp.sdk.ApiClient
import co.bdozer.core.nlp.sdk.api.DefaultApi
import co.bdozer.core.nlp.sdk.model.ZeroShotClassificationRequest
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayDeque

class TenKSectionExtractor {

    private val log = LoggerFactory.getLogger(TenKSectionExtractor::class.java)
    private val apiClient = ApiClient()

    init {
        apiClient.basePath = System.getenv("CORE_NLP_ENDPOINT") ?: "http://localhost:8000"
    }

    private val coreNlp = apiClient.buildClient(DefaultApi::class.java)

    /**
     * Take a 10-K document and find the
     * Item 1, 1A sections DOM elements from it
     */
    fun extractSections(doc: Document): TenKSections {
        val tables = doc.select("table").take(20)
        val tableOfContent = tables.maxByOrNull { scoreTable(it) }

        val business = tableRowToSection(tableOfContent, "Item Business")
        val riskFactors = tableRowToSection(tableOfContent, "Risk Factors")

        return TenKSections(
            business = business.copy(elements = findElements(doc, business)),
            riskFactors = riskFactors.copy(elements = findElements(doc, business)),
        )
    }

    /**
     * Take a TOC of a 10-K and figure out the anchor and element ID for different sections
     * such as Item 1 Business, Item 1A Risk Factors
     */
    private fun tableRowToSection(table: Element?, name: String): TenKSection {
        log.info("Finding anchor tag for name=$name")
        val rows = table?.select("tr")
        val tgtRow = rows?.map {
            val response = coreNlp.zeroShotClassification(
                ZeroShotClassificationRequest()
                    .sentence(name)
                    .candidateLabels(listOf(it.text()))
            )
            it to response.result.first().score
        }?.maxByOrNull { it.second }?.first

        var idx = (rows?.indexOf(tgtRow) ?: 0) + 1
        fun hasHref(idx: Int): String? {
            val href = rows
                ?.get(idx)
                ?.select("a")
                ?.attr("href")
            return if (href?.isBlank() == true) {
                null
            } else {
                href
            }
        }

        val size = rows?.size ?: 0
        var endAnchor: String? = null
        while (idx < size) {
            endAnchor = hasHref(idx)
            if (endAnchor != null) {
                break
            } else {
                idx++
            }
        }

        val startAnchor = tgtRow?.select("a")?.attr("href")
        return TenKSection(startAnchor = startAnchor, endAnchor = endAnchor, name = name)
    }

    /**
     * Returns the overall score of how likely this is the table of content
     */
    private fun scoreTable(table: Element): Double {
        val expectedRows = listOf(
            "Item Business",
            "1. Business",
            "Item Risk Factors",
            "1A Risk Factors",
            "Item Unresolved Staff Comments",
        )
        // find max score for each expected row them sum them to 
        // determine the relevance of the entire table
        val score = expectedRows.sumOf { expectedRow ->
            val tableRows = table.select("tr").map { it.text() }
            val response = coreNlp.zeroShotClassification(
                ZeroShotClassificationRequest()
                    .sentence(expectedRow)
                    .candidateLabels(tableRows)
            )
            response.result.maxOf { it.score.toDouble() }
        }
        return score

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

}
