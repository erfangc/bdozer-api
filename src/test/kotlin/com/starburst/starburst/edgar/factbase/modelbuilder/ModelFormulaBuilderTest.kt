package com.starburst.starburst.edgar.factbase.modelbuilder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilder
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.Model
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

internal class ModelFormulaBuilderTest {

    @Test
    fun buildModelFormula() {
        val om = jacksonObjectMapper().findAndRegisterModules()
        val ctx =
            om.readValue<ModelFormulaBuilderContext>(ClassPathResource("factbase/sample/${ModelFormulaBuilderContext::class.java.simpleName}.json").inputStream)
        val model =
            om.readValue<Model>(ClassPathResource("factbase/sample/${Model::class.java.simpleName}.json").inputStream)
        val output = ModelFormulaBuilder(ctx = ctx, model = model).buildModelFormula()
        println(output)
    }
}