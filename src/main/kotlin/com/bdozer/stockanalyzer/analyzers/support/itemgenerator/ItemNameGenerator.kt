package com.bdozer.stockanalyzer.analyzers.support.itemgenerator

import com.bdozer.edgar.factbase.dataclasses.Fact

class ItemNameGenerator {

    fun itemName(fact: Fact): String {
        return if (fact.explicitMembers.isEmpty()) {
            fact.conceptName
        } else {
            val dimensions = fact
                .explicitMembers
                .sortedByDescending { it.dimension }
                .joinToString(",") { "${it.dimension}=${it.value}" }
            "${fact.conceptName}[$dimensions]"
        }
    }

    fun itemName(conceptName: String): String {
        return conceptName
    }

}