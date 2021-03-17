package com.starburst.starburst.xml

import com.starburst.starburst.xml.NodeListExtension.toList
import org.w3c.dom.Element

/**
 * A delegated class that holds some convenience methods for working with and traversing
 * XML [Element]. Such as creating namespace resolution methods and maps
 */
class XmlElement(element: Element) : Element by element {

    private val longNamespaceToShortNamespaceMap = this.longNamespaceToShortNamespaceMap()
    private val defaultLongNamespace = this.defaultLongNamespace()

    fun childNodes(): List<XmlNode> {
        return this.childNodes.toList().map { XmlNode(it, this) }
    }

    fun getShortNamespace(longNamespace: String): String? {
        return if (this.defaultLongNamespace == longNamespace) {
            null
        } else {
            longNamespaceToShortNamespaceMap[longNamespace]
        }
    }

    /**
     * Maps long namespace to the shorter namespace declarations
     */
    fun longNamespaceToShortNamespaceMap(): Map<String, String> {
        val length = this.attributes.length
        val namespaces = mutableMapOf<String, String>()
        for (i in 0 until length) {
            val attribute = this.attributes.item(i)
            if (attribute.nodeName.startsWith("xmlns:")) {
                val shortNamespace = attribute.nodeName.split(":".toRegex(), 2).last()
                val longNamespace = attribute.textContent
                namespaces[longNamespace] = shortNamespace
            }
        }
        return namespaces.toMap()
    }

    /**
     * Maps short namespace to the longer namespace declarations
     */
    fun shortNamespaceToLongNamespaceMap(): Map<String, String> {
        val length = this.attributes.length
        val namespaces = mutableMapOf<String, String>()
        for (i in 0 until length) {
            val attribute = this.attributes.item(i)
            if (attribute.nodeName.startsWith("xmlns:")) {
                val shortNamespace = attribute.nodeName.split(":".toRegex(), 2).last()
                val longNamespace = attribute.textContent
                namespaces[shortNamespace] = longNamespace
            }
        }
        return namespaces.toMap()
    }

    /**
     * Return the target namespace of the root element
     */
    fun targetNamespace(): String? {
        return this.attributes.getNamedItem("targetNamespace")?.textContent
    }

    /**
     * Returns the default xmlns in short form
     */
    fun defaultShortNamespace(): String? {
        return longNamespaceToShortNamespaceMap[defaultLongNamespace]
    }

    /**
     * Returns the default xmlns in long form
     */
    fun defaultLongNamespace(): String? {
        return this.attributes.getNamedItem("xmlns")?.textContent
    }

    /**
     * look at xmlns declarations and return a list of namespaces
     * for this XML
     */
    fun getShortNamespaces(): List<String> {
        val length = this.attributes.length
        val namespaces = mutableListOf<String>()
        for (i in 0 until length) {
            val item = this.attributes.item(i)
            if (item.nodeName.startsWith("xmlns:")) {
                val namespace = item.nodeName.split(":".toRegex(), 2).last()
                namespaces.add(namespace)
            }
        }
        return namespaces.toList()
    }

    fun getElementsByTag(namespace: String, tag: String): List<XmlNode> {
        val newTag = longNamespaceToShortNamespaceMap[namespace]?.let { "$it:$tag" } ?: tag
        return this.getElementsByTag(newTag)
    }

    fun getElementsByTag(tag: String): List<XmlNode> {
        return childNodes().filter { it.nodeName == tag }
    }

    fun getElementByTag(tag: String): XmlNode? {
        val nodeList = this.getElementsByTagName(tag)
        return if (nodeList.length > 0) {
            nodeList.item(0)?.let { XmlNode(it, this) }
        } else {
            null
        }
    }

    fun getElementByTag(namespace: String, tag: String): XmlNode? {
        val newTag = longNamespaceToShortNamespaceMap[namespace]?.let { "$it:$tag" } ?: tag
        return this.getElementByTag(newTag)
    }

}