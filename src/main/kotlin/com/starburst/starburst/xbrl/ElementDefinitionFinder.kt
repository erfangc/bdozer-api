package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.utils.NodeListExtension.toList
import com.starburst.starburst.xbrl.dataclasses.ElementDefinition
import com.starburst.starburst.xbrl.dataclasses.XbrlUtils
import org.w3c.dom.NodeList
import java.io.InputStream

class ElementDefinitionFinder(gaapXsd: InputStream, extXsd: InputStream) {

    private fun NodeList.associateElementsbyId(): Map<String, ElementDefinition> {
        return this
            .toList()
            .map {
                it.attributes.getNamedItem("id").textContent to
                        ElementDefinition(
                            name = it.attributes.getNamedItem("name").textContent,
                            type = it.attributes.getNamedItem("type")?.textContent ?: "",
                            periodType = it.attributes.getNamedItem(":xbrli:periodType")?.textContent ?: "",
                        )
            }
            .toMap()
    }

    private val xml1 = XbrlUtils.readXml(gaapXsd)
    private val gaapXml = xml1
        .getElementsByTagName("xs:element")
        .associateElementsbyId()

    private val xml2 = XbrlUtils.readXml(extXsd)
    private val extXml = xml2
        .getElementsByTagName("xs:element")
        .associateElementsbyId()

    /**
     * Find the element definition in  the XSD files, first look in extension if not found then look in usGaap
     */
    fun lookupElementDefinition(elementId: String): ElementDefinition {
        // TODO figure out the correct namespace
        return extXml[elementId]?.copy(namespace = "dbx")
            ?: gaapXml[elementId]
            ?: error("cannot find element definition for $elementId")
    }

}
