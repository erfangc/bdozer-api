package com.starburst.starburst.edgar.factbase.modelbuilder.helper

import com.starburst.starburst.xml.XmlNode
import java.net.URI

object RoleRefsExtensions {

    fun List<XmlNode>.findCashFlowStatementRole(): String {
        return this.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && ((last.contains("cash") && last.contains("flow")))

        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }

    fun List<XmlNode>.findBalanceSheetRole(): String {
        return this.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && (
                    (last.contains("balance") && last.contains("sheet"))
                            || (last.contains("financial") && last.contains("condition"))
                    )
        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }

    fun List<XmlNode>.findIncomeStatementRole(): String {
        return this.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && (
                    last.contains("earning")
                            || last.contains("income")
                            || last.contains("ofincome")
                            || last.contains("ofearning")
                            || last.contains("ofoperation")
                    )
        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find income statement role")
    }

}