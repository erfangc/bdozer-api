package com.bdozer.edgar.factbase.modelbuilder.formula

import com.bdozer.edgar.dataclasses.Concept
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.models.dataclasses.Model

data class ModelFormulaBuilderContext(
    val facts: List<Fact>,
    val conceptMap: Map<String, Concept>,
    val flattenedItemDependencyGraph: Map<String, Set<String>>,
    val itemDependencyGraph: Map<String, List<String>>,
    val model: Model,
) {
    fun isDependentOn(item: String, parentItem: String): Boolean {
        return flattenedItemDependencyGraph[parentItem]?.contains(item) == true ||
                item == parentItem
    }
}