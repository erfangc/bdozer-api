package com.starburst.starburst.edgar.factbase.modelbuilder.factory

import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilder
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.skeletongenerator.SkeletonGenerator
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.dataclasses.Model
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ModelFactory(
    private val filingProviderFactory: FilingProviderFactory,
    private val modelFormulaBuilder: ModelFormulaBuilder,
    private val factBase: FactBase
) {

    private val log = LoggerFactory.getLogger(ModelFactory::class.java)

    private fun skeletonGenerator(cik: String, adsh: String): SkeletonGenerator {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        return SkeletonGenerator(
            filingProvider = filingProvider,
            schemaManager = SchemaManager(filingProvider),
            facts = factBase.getFacts(cik = filingProvider.cik())
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
        val skeletonGenerator = skeletonGenerator(cik, adsh)

        val ctx = ModelFormulaBuilderContext(
            facts = skeletonGenerator.facts,
            conceptDefinitionMap = skeletonGenerator.elementDefinitionMap,
            itemDependencyGraph = skeletonGenerator.itemDependencyGraph,
            flattenedItemDependencyGraph = skeletonGenerator.flattenedItemDependencyGraph,
            model = skeletonGenerator.model
        )

        val model = modelFormulaBuilder.buildModel(ctx)
        log.info("Finished building model ${model.cik} using FactBase facts")
        return model
    }

}