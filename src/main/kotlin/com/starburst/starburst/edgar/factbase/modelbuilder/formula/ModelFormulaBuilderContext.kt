package com.starburst.starburst.edgar.factbase.modelbuilder.formula

import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.models.Model

data class ModelFormulaBuilderContext(
    val facts: List<Fact>,
    val elementDefinitionMap: Map<String, ElementDefinition>,
    val itemDependencyGraph: Map<String, List<String>>,
    val model:Model,
)