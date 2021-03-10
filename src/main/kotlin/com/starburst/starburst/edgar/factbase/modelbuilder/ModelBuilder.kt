package com.starburst.starburst.edgar.factbase.modelbuilder

import com.fasterxml.jackson.databind.ObjectMapper
import com.starburst.starburst.edgar.XbrlConstants.link
import com.starburst.starburst.edgar.XbrlConstants.xlink
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilder
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.edgar.factbase.modelbuilder.helper.ModelBuilderHelper
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.xml.XmlNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service(value = "factBaseModelBuilder")
class ModelBuilder(
    private val filingProviderFactory: FilingProviderFactory,
    private val factBase: FactBase,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(ModelBuilder::class.java)

    private fun createHelper(cik: String, adsh: String): ModelBuilderHelper {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        return ModelBuilderHelper(
            calculationLinkbase = filingProvider.calculationLinkbase(),
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
        val helper = createHelper(cik, adsh)

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = helper.entityRegistrantName()
        val symbol = helper.tradingSymbol()

        val model = Model(
            name = name,
            symbol = symbol,
            incomeStatementItems = helper.incomeStatementCalculationItems,
            balanceSheetItems = helper.balanceSheetCalculationItems,
            cashFlowStatementItems = helper.cashFlowStatementItems,
        )

        val ctx = ModelFormulaBuilderContext(
            facts = helper.facts,
            elementDefinitionMap = helper.elementDefinitionMap,
            itemDependencyGraph = helper.itemDependencyGraph,
            model = model
        )

        // serialize the ctx and model for unit test - comment out when not in use
        val formulatedModel = ModelFormulaBuilder(model, ctx).buildModelFormula()

        if (true) {
            objectMapper.writeValue(
                File("src/test/resources/factbase/sample/${ctx.javaClass.simpleName}.json"),
                ctx
            )
            objectMapper.writeValue(
                File("src/test/resources/factbase/sample/${model.javaClass.simpleName}.json"),
                model
            )
        }

        //
        // TODO if CF statement does not start with something from the income statement, then we need to manually fix the linkage
        // ex: GS in CF statement uses us-gaap:ProfitLoss whereas the last line in income statement is NetIncomeLoss
        //
        log.info("Finished building model ${formulatedModel.cik} using FactBase facts")
        return formulatedModel
    }

    /**
     * linkCalculations are grouping of calculation arcs that specifies the calculation
     * of a single table on the financial statement report such as an income statement or balance-sheet
     */
    private fun linkCalculationToItems(calculationLinks: XmlNode, ctx: ModelBuilderHelper): List<Item> {
        /*
        to build the income statement, first find all the loc elements
         */
        val locs = calculationLinks.getElementsByTag(link, "loc")
        val locsLookup = locs.associateBy { it.attr(xlink, "label") }
        val calculationArcs = calculationLinks.getElementsByTag(link, "calculationArc")
        TODO()
    }

}