package com.starburst.starburst.xbrl.utils

import org.w3c.dom.Element
import org.w3c.dom.NodeList

object ElementExtension {

    /**
     * look at xmlns declarations and return a list of namespaces
     * for this XML
     */
    private fun Element.getNamespaces(): List<String> {
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

    /**
     * This method calls [Element.getElementsByTagName] with the namespace given in [tag]
     * and if that fails it goes through a waterfall of other tags to use depending on
     * the namespace that has been declared on the root element
     */
    fun Element.getElementsByTagNameSafe(tag: String): NodeList {
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
                val namespaces = this.getNamespaces()
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
