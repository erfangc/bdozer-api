package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.factbase.Fact
import com.starburst.starburst.models.dataclasses.Model

data class ModelFormulaBuilderContext(
    val facts: List<Fact>,
    val elementDefinitionMap: Map<String, ElementDefinition>,
    val flattenedItemDependencyGraph: Map<String, Set<String>>,
    val itemDependencyGraph: Map<String, List<String>>,
    val model: Model,
) {
    fun isDependentOn(item: String, parentItem: String): Boolean {
        return flattenedItemDependencyGraph[parentItem]?.contains(item) == true ||
                item == parentItem
    }
}