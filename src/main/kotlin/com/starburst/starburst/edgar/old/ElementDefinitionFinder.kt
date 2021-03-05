package com.starburst.starburst.edgar.old

import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.XbrlUtils
import com.starburst.starburst.edgar.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream

/**
 * The goal is to take the standard and extension XSDs of the filing
 * and determine for any given tag what it's value should be
 */
class ElementDefinitionFinder(gaapXsd: InputStream, extXsd: InputStream) {

    private fun NodeList.associateElementsbyId(namespace: String = "us-gaap"): Map<String, ElementDefinition> {
        return this
            .toList()
            .map {
                val id = it.attributes.getNamedItem("id").textContent
                id to
                        ElementDefinition(
                            id = id,
                            longNamespace = namespace,
                            name = it.attributes.getNamedItem("name").textContent,
                            type = it.attributes.getNamedItem("type")?.textContent ?: "",
                            periodType = it.attributes.getNamedItem(":xbrli:periodType")?.textContent ?: "",
                        )
            }
            .toMap()
    }

    private val xml1 = XbrlUtils.readXml(gaapXsd)
    private val gaapElements = xml1
        .getElementsByTagNameSafe("xs:element")
        .associateElementsbyId()

    private val xml2 = XbrlUtils.readXml(extXsd)

    private val extensionElements = xml2
        .getElementsByTagNameSafe("xs:element")
        .associateElementsbyId(namespace = targetNamespaceShort(xml2))

    private fun targetNamespaceShort(element: Element): String {
        val attributes = element.attributes
        val targetNamespace = attributes.getNamedItem("targetNamespace").textContent
        val idx = (0 until attributes.length)
            .first {
                attributes.item(it).textContent == targetNamespace && attributes.item(it).nodeName != "targetNamespace"
            }
        return attributes.item(idx).nodeName.replace("xmlns:", "")
    }

    /**
     * Find the element definition in  the XSD files, first look in extension if not found then look in usGaap
     */
    fun lookupElementDefinition(elementId: String): ElementDefinition {
        val extensionDefinition = extensionElements[elementId]
        val gaapDefinition = gaapElements[elementId]
        return extensionDefinition
            ?: gaapDefinition
            ?: error("cannot find element definition for $elementId")
    }

}
