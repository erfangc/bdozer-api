package com.bdozer.sec.factbase

import com.bdozer.sec.XbrlNamespaces
import com.bdozer.xml.XmlNode

object XLinkExtentions {
    fun XmlNode?.label(): String? =
        this?.attr(XbrlNamespaces.xlink, "label")

    fun XmlNode?.arcrole(): String? = this?.attr(XbrlNamespaces.xlink, "arcrole")

    fun XmlNode?.type(): String? = this?.attr(XbrlNamespaces.xlink, "type")

    fun XmlNode?.weight() = this?.attr("weight")?.toDoubleOrNull() ?: 0.0

    fun XmlNode?.order() = this?.attr("order")?.toIntOrNull()

    fun XmlNode?.href(): String? =
        this?.attr(XbrlNamespaces.xlink, "href")

    fun XmlNode?.role(): String? =
        this?.attr(XbrlNamespaces.xlink, "role")

    fun XmlNode?.from(): String? =
        this?.attr(XbrlNamespaces.xlink, "from")

    fun XmlNode?.to(): String? =
        this?.attr(XbrlNamespaces.xlink, "to")
}