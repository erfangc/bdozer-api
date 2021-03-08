package com.starburst.starburst.edgar.utils

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.utils.HttpClientExtensions.readXml
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import org.apache.http.impl.client.HttpClientBuilder
import org.w3c.dom.Node
import org.w3c.dom.NodeList

object NodeListExtension {

    fun Node.getElementByTag(tag: String): Node? {
        return this.childNodes.toList().firstOrNull { it.nodeName ==  tag}
    }

    fun Node.getElementsByTag(tag: String): List<Node> {
        return this.childNodes.toList().filter { it.nodeName ==  tag}
    }

    fun Node.getElementByTag(namespace: String, tag: String): Node? {
        val myTag = XmlElement(this.ownerDocument.documentElement).getShortNamespace(namespace)?.let { "$it:$tag" } ?: tag
        return this.childNodes.toList().firstOrNull { it.nodeName ==  myTag}
    }

    fun Node.getElementsByTag(namespace: String, tag: String): List<Node> {
        val myTag = XmlElement(this.ownerDocument.documentElement).getShortNamespace(namespace)?.let { "$it:$tag" } ?: tag
        return this.childNodes.toList().filter { it.nodeName ==  myTag}
    }

    fun Node.attr(attr: String): String? {
        return this.attributes.getNamedItem(attr)?.textContent
    }

    fun Node.attr(namespace: String, attr: String): String? {

        // create a map of long -> short namespaces
        val nsMap = mutableMapOf<String, String>()
        for (i in (0 until this.attributes.length)) {
            val item = this.attributes.item(i)
            if (item.nodeName.startsWith("xmlns:")) {
                val short = item.nodeName.split(":".toRegex(), 2).last()
                val long = item.textContent
                nsMap[long] = short
            }
        }

        val fullAttr = nsMap[namespace]?.let { "$it:$attr" } ?: attr

        return this.attributes.getNamedItem(fullAttr)?.textContent
    }

    fun <R> NodeList.map(transform: (Node) -> R): List<R> {
        val results = mutableListOf<R>()
        for (i in 0 until this.length) {
            val node = this.item(i)
            results.add(transform.invoke(node))
        }
        return results.toList()
    }

    fun NodeList.toList(): List<Node> {
        val results = mutableListOf<Node>()
        for (i in 0 until this.length) {
            val node = this.item(i)
            results.add(node)
        }
        return results.toList()
    }

}

