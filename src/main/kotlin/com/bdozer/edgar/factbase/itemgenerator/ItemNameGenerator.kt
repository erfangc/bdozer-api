package com.bdozer.edgar.factbase.itemgenerator

import com.bdozer.edgar.factbase.dataclasses.Fact

class ItemNameGenerator {

    fun itemName(fact: Fact): String {
        return if (fact.explicitMembers.isEmpty()) {
            fact.conceptName
        } else {
            val dimensions = fact
                .explicitMembers
                .sortedByDescending { explicitMember -> explicitMember.dimension }
                .joinToString("_") { explicitMember ->
                    val dimension = explicitMember.dimension.replace(":", "_").replace("-", "_")
                    val value = explicitMember.value.replace(":", "_").replace("-", "_")
                    "${dimension}_$value"
                }
            "${fact.conceptName}_$dimensions"
        }
    }

    fun itemName(conceptName: String): String {
        return conceptName
    }

}