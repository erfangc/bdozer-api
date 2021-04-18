package com.bdozer.stockanalyzer.itemgenerator

import com.bdozer.edgar.factbase.dataclasses.Fact

class ItemNameGenerator {

    fun itemName(fact: Fact): String {
        return if (fact.explicitMembers.isEmpty()) {
            fact.conceptName
        } else {
            val dimensions = fact
                .explicitMembers
                .sortedByDescending { it.dimension }
                .joinToString("_") { "${it.dimension.replace(":","_")}_${it.value.replace(":","_")}" }
            "${fact.conceptName}_$dimensions"
        }
    }

    fun itemName(conceptName: String): String {
        return conceptName
    }

}