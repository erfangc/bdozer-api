package com.bdozer.api.factbase.core.xml

import org.w3c.dom.Node
import org.w3c.dom.NodeList

object NodeListExtension {

    fun NodeList.toList(): List<Node> {
        val results = mutableListOf<Node>()
        for (i in 0 until this.length) {
            val node = this.item(i)
            results.add(node)
        }
        return results.toList()
    }

}

