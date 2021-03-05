package com.starburst.starburst.edgar.utils

import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object XbrlUtils {
    fun readXml(source: InputStream): Element {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(source).documentElement
    }
}
