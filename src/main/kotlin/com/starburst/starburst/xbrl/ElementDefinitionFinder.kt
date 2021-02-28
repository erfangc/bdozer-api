package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.dataclasses.ElementDefinition
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils
import com.starburst.starburst.xbrl.utils.NodeListExtension.toList
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream

class ElementDefinitionFinder(gaapXsd: InputStream, extXsd: InputStream) {

    private fun NodeList.associateElementsbyId(namespace: String = "us-gaap"): Map<String, ElementDefinition> {
        return this
            .toList()
            .map {
                it.attributes.getNamedItem("id").textContent to
                        ElementDefinition(
                            namespace = namespace,
                            name = it.attributes.getNamedItem("name").textContent,
                            type = it.attributes.getNamedItem("type")?.textContent ?: "",
                            periodType = it.attributes.getNamedItem(":xbrli:periodType")?.textContent ?: "",
                        )
            }
            .toMap()
    }

    private val xml1 = XbrlUtils.readXml(gaapXsd)
    private val gaapElements = xml1
        .getElementsByTagName("xs:element")
        .associateElementsbyId()

    private val xml2 = XbrlUtils.readXml(extXsd)

    private val extensionElements = xml2
        .getElementsByTagName("xs:element")
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
