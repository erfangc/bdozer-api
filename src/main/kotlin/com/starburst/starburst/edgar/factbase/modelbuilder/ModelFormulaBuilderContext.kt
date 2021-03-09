package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.Fact

data class ModelFormulaBuilderContext(
    val facts: List<Fact>,
    val elementDefinitionMap: Map<String, ElementDefinition>,
    val itemDependencyGraph: Map<String, List<String>>,
)