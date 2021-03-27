package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.dataclasses.Concept
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.models.dataclasses.Model

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