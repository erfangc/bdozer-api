package com.starburst.starburst.edgar.factbase.modelbuilder.factory

import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilder
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.builder.FactBaseModelBuilder
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.Model
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ModelFactory(
    private val filingProviderFactory: FilingProviderFactory,
    private val modelFormulaBuilder: ModelFormulaBuilder,
    private val factBase: FactBase
) {

    private val log = LoggerFactory.getLogger(ModelFactory::class.java)

    private fun createBuilder(cik: String, adsh: String): FactBaseModelBuilder {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        return FactBaseModelBuilder(
            filingProvider = filingProvider,
            schemaManager = SchemaManager(filingProvider),
            facts = factBase.allFactsForCik(cik = filingProvider.cik())
        )
    }

    /**
     * Build a [Model] using facts from [FactBase] and the calculationArcs
     * defined by a specific filing
     */
    fun buildModelForFiling(cik: String, adsh: String): Model {
        /*
        high levels overview
         */
        val builder = createBuilder(cik, adsh)

        val ctx = ModelFormulaBuilderContext(
            facts = builder.facts,
            elementDefinitionMap = builder.elementDefinitionMap,
            itemDependencyGraph = builder.itemDependencyGraph,
            model = builder.model
        )

        // serialize the ctx and model for unit test - comment out when not in use
        val formulatedModel = modelFormulaBuilder.buildModelFormula(ctx)

        log.info("Finished building model ${formulatedModel.cik} using FactBase facts")
        return formulatedModel
    }

}