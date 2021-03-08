package com.starburst.starburst.edgar.utils

import com.starburst.starburst.edgar.XmlElement
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object XbrlUtils {
    fun readXml(source: InputStream): XmlElement {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return XmlElement(builder.parse(source).documentElement)
    }
}
