package com.starburst.starburst.edgar.utils

import org.w3c.dom.Node
import org.w3c.dom.NodeList

object NodeListExtension {

    fun Node.getElementByTag(tag: String): Node? {
        return this.childNodes.toList().firstOrNull { it.nodeName ==  tag}
    }

    fun Node.getElementsByTag(tag: String): List<Node> {
        return this.childNodes.toList().filter { it.nodeName ==  tag}
    }

    fun Node.attr(attr: String): String? {
        return this.attributes.getNamedItem(attr)?.textContent
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

