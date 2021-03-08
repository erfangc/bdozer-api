package com.starburst.starburst.xml

import com.starburst.starburst.xml.NodeListExtension.toList
import org.w3c.dom.Node

/**
 * A delegated class that holds convenience methods for working with XML nodes
 * that are not the root element, it keeps a reference to the owning [XmlElement]
 * so we can look up namespaces conveniently and easily
 */
class XmlNode(
    private val node: Node,
    val document: XmlElement
) : Node by node {

    fun childNodes(): List<XmlNode> {
        return this.childNodes.toList().map { XmlNode(it, document) }
    }

    fun getElementByTag(tag: String): XmlNode? {
        return childNodes().firstOrNull { it.nodeName == tag }
    }

    fun getElementsByTag(tag: String): List<XmlNode> {
        return childNodes().toList().filter { it.nodeName == tag }
    }

    fun getElementByTag(namespace: String, tag: String): XmlNode? {
        val myTag =
            XmlElement(this.ownerDocument.documentElement).getShortNamespace(namespace)?.let { "$it:$tag" } ?: tag
        return this.childNodes().firstOrNull { it.nodeName == myTag }
    }

    fun getElementsByTag(namespace: String, tag: String): List<XmlNode> {
        val myTag =
            XmlElement(this.ownerDocument.documentElement).getShortNamespace(namespace)?.let { "$it:$tag" } ?: tag
        return childNodes().toList().filter { it.nodeName == myTag }
    }

    fun attr(attr: String): String? {
        return this.attributes?.getNamedItem(attr)?.textContent
    }

    fun attr(namespace: String, attr: String): String? {

        //
        // see if xmlns are declared on this element, otherwise fallback to the document xmlns declarations
        // create a map of long -> short namespaces
        //
        val localNsMap = mutableMapOf<String, String>()
        for (i in (0 until this.attributes.length)) {
            val item = this.attributes.item(i)
            if (item.nodeName.startsWith("xmlns:")) {
                val short = item.nodeName.split(":".toRegex(), 2).last()
                val long = item.textContent
                localNsMap[long] = short
            }
        }

        val fullAttr = (localNsMap[namespace] ?: document.getShortNamespace(namespace))?.let { "$it:$attr" } ?: attr

        return this.attr(fullAttr)
    }
}