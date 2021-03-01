package com.starburst.starburst.xbrl.utils

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

object NodeListExtension {

    fun Node.findByTag(tag: String): Node? {
        return this.childNodes.toList().firstOrNull { it.nodeName ==  tag}
    }

    fun Node.findAllByTag(tag: String): List<Node> {
        return this.childNodes.toList().filter { it.nodeName ==  tag}
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

object ElementExtension {

    fun Element.getNamespaces(): List<String> {
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

    fun Element.getElementsByTagNameSafe(tag: String): NodeList {
        val nl = this.getElementsByTagName(tag)
        if (nl.length == 0) {
            val parts = tag.split(":".toRegex(), 2)
            val namespace = if (parts.size > 1) parts.first() else ""
            val elementTag = if (parts.size > 1) parts.last() else parts.first()

            // first try it without the namespaec
            val attempt1 = this.getElementsByTagName(elementTag)

            if (attempt1.length == 0) {
                //
                // figure out if the requested tag is declared in the namespace
                // if not, get rid of it - or try one of the other namespaces
                //
                val namespaces = this.getNamespaces()
                if (namespace.isNotEmpty() && !namespaces.contains(namespace)) {
                    // try out other namespaces
                    for (ns in namespaces) {
                        val nodeList = this.getElementsByTagName("$ns:$elementTag")
                        if (nodeList.length != 0) {
                            return nodeList
                        }
                    }
                    return attempt1
                } else {
                    return attempt1
                }
            } else {
                return attempt1
            }


        } else {
            return nl
        }
    }
}
