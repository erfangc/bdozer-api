package com.starburst.starburst.edgar

import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

/**
 * A delegated class that holds some convenience methods for working with and traversing
 * XML [Element]. Such as creating namespace resolution methods and maps
 */
class XmlElement(element: Element): Element by element {

    private val shortNamespaces = this.getShortNamespaces()
    private val longNamespaceToShortNamespaceMap = this.longNamespaceToShortNamespaceMap()
    private val shortNamespaceToLongNamespaceMap = this.shortNamespaceToLongNamespaceMap()
    private val defaultLongNamespace = this.defaultLongNamespace()

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
     *
     */
    fun targetNamespace(): String? {
        return this.attributes.getNamedItem("targetNamespace")?.textContent
    }

    /**
     *
     */
    fun defaultShortNamespace(): String? {
        return longNamespaceToShortNamespaceMap[defaultLongNamespace]
    }

    /**
     *
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

    fun getElementsByTag(namespace: String, tag: String): List<Node> {
        val ns = longNamespaceToShortNamespaceMap[namespace]?.let { "$it:$tag" } ?: tag
        return this.getElementsByTag(ns)
    }

    fun getElementByTag(tag: String): Node? {
        val nodeList = this.getElementsByTagName(tag)
        return if (nodeList.length > 0) {
            nodeList.item(0)
        } else {
            null
        }
    }

    /**
     * This method calls [Element.getElementsByTagName] with the namespace given in [tag]
     * and if that fails it goes through a waterfall of other tags to use depending on
     * the namespace that has been declared on the root element
     */
    fun getElementsByTagNameSafe(tag: String): NodeList {
        // to save on algo complexity - just call [getElementsByTagName] as you normally would
        val nl = this.getElementsByTagName(tag)
        // when that fails, think about whether to apply namespaces
        if (nl.length == 0) {
            val parts = tag.split(":".toRegex(), 2)
            val namespace = if (parts.size > 1) parts.first() else ""
            val elementTag = if (parts.size > 1) parts.last() else parts.first()

            // first try it without the namespace
            val attemptWithoutNamespace = this.getElementsByTagName(elementTag)

            if (attemptWithoutNamespace.length == 0) {
                //
                // figure out if the requested tag is declared in the namespace
                // if not, get rid of it - or try one of the other namespaces
                //
                val namespaces = this.shortNamespaces
                if (namespace.isNotEmpty() && !namespaces.contains(namespace)) {
                    // try out other namespaces that has been declared
                    for (newNameSpace in namespaces) {
                        val nodeList = this.getElementsByTagName("$newNameSpace:$elementTag")
                        if (nodeList.length != 0) {
                            return nodeList
                        }
                    }
                    return attemptWithoutNamespace
                } else {
                    return attemptWithoutNamespace
                }
            } else {
                return attemptWithoutNamespace
            }
        } else {
            return nl
        }
    }
}