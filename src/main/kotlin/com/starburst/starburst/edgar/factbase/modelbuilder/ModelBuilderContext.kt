package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.xml.XmlElement

data class ModelBuilderContext(
    val calculationLinkbase: XmlElement,
    val schemaManager: SchemaManager,
    val latestNonDimensionalFacts: Map<String, Fact>,
    val facts: List<Fact>
)