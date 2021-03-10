package com.starburst.starburst.edgar.factbase.modelbuilder

import com.fasterxml.jackson.databind.ObjectMapper
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.xml.XmlNode
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI

@Service(value = "factBaseModelBuilder")
class ModelBuilder(
    private val filingProviderFactory: FilingProviderFactory,
    private val factBase: FactBase,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        const val link = "http://www.xbrl.org/2003/linkbase"
        const val xlink = "http://www.w3.org/1999/xlink"
    }

    /**
     * Build a [Model] using facts from [FactBase] and the calculationArcs
     * defined by a specific filing
     */
    fun buildModelForFiling(cik: String, adsh: String): Model {
        /*
        high levels overview
         */
        val ctx = buildContext(cik, adsh)

        /*
         SEC require - XBRL
            - XSD, term concept definition (ex: blk:AladdinRevenue)
            - Instance Document (all the facts flattened out, Revenue, AladdinRevenue, ProvisionForLosses, NetIncome, OperatingCashFlow, BalanceSheet etc.)
            - Def File -> define parent child relationships between concepts
            - Pres File -> How to display (in which order to render - which one to bold or show as subtotal) (AladdinRevenue comes from Total Revenue, which comes before Gross Profit)
            - Calculation File -> Income Statement / Balance Sheet / Cash Flow Statement (some times other misc.) -> which Concept add up to which one ....
         */

        /*
        step 1 - start with the calculation XML since
        from there we derive the structure of the `Model` to be created
        everything else serves as a reference (including "XBRL facts" such as actual historical values)
        to the business logic expressed within this page
         */
        val calculationLinks = ctx.calculationLinkbase.getElementsByTag(link, "calculationLink")

        /*
        find the income statement calculation
         */
        val incomeStatementRole =
            findIncomeStatementRole(ctx.calculationLinkbase.getElementsByTag(link, "roleRef"))
        val incomeStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, incomeStatementRole), ctx)

        /*
        find the income statement calculation
         */
        val balanceSheetRole = findBalanceSheetRole(ctx.calculationLinkbase.getElementsByTag(link, "roleRef"))
        val balanceSheetItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, balanceSheetRole), ctx)

        /*
        find the cash flow statement calculation
         */
        val cashFlowStatementRole =
            findCashFlowStatementRole(ctx.calculationLinkbase.getElementsByTag(link, "roleRef"))
        val cashFlowStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, cashFlowStatementRole), ctx)

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = ctx.entityRegistrantName()
        val symbol = ctx.tradingSymbol()

        val model = Model(
            name = name,
            symbol = symbol,
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = cashFlowStatementItems,
            otherItems = emptyList(),
        )

        val formulaBuilderContext = ModelFormulaBuilderContext(
            facts = ctx.facts,
            elementDefinitionMap = ctx.elementDefinitionMap,
            itemDependencyGraph = ctx.itemDependencyGraph
        )

        // serialize the ctx and model for unit test - comment out when not in use
//        objectMapper.writeValue(
//            File("src/test/resources/factbase/sample/${formulaBuilderContext.javaClass.simpleName}.json"),
//            formulaBuilderContext
//        )
//        objectMapper.writeValue(
//            File("src/test/resources/factbase/sample/${model.javaClass.simpleName}.json"),
//            model
//        )

        //
        // TODO remove duplicated items across the 3 statements, keep the one with linkage
        // for example, NetIncomeLoss appears on both the income statement and cash flow statement
        // remove it from places where it has no other dependencies
        //

        //
        // TODO if CF statement does not start with something from the income statement, then we need to manually fix the linkage
        // ex: GS in CF statement uses us-gaap:ProfitLoss whereas the last line in income statement is NetIncomeLoss
        //

        val builder = ModelFormulaBuilder(model, formulaBuilderContext).buildModelFormula()
        return model
    }

    private fun findCashFlowStatementRole(nodes: List<XmlNode>): String {
        return nodes.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && ((last.contains("cash") && last.contains("flow")))

        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }


    private fun buildContext(cik: String, adsh: String): ModelBuilderContext {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        return ModelBuilderContext(
            calculationLinkbase = filingProvider.calculationLinkbase(),
            schemaManager = SchemaManager(filingProvider),
            facts = factBase.allFactsForCik(cik = filingProvider.cik())
        )
    }

    private fun findBalanceSheetRole(nodes: List<XmlNode>): String {
        return nodes.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && (
                    (last.contains("balance") && last.contains("sheet"))
                            || (last.contains("financial") && last.contains("condition"))
                    )
        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }

    private fun findIncomeStatementRole(nodes: List<XmlNode>): String {
        return nodes.first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && (
                    last.contains("earning")
                            || last.contains("income")
                            || last.contains("ofincome")
                            || last.contains("ofearning")
                            || last.contains("ofoperation")
                    )
        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find income statement role")
    }

    private fun findLinkCalculationByRole(calculationLinks: List<XmlNode>, role: String): XmlNode {
        val list = calculationLinks.toList()
        return list
            .find {
                it.attr(xlink, "role") == role
            }
            ?: error("cannot find $role")
    }

    private fun linkCalculationToItems(incomeStatementCalculation: XmlNode, ctx: ModelBuilderContext): List<Item> {
        //
        // to build the income statement, first find all the loc elements
        //
        val locs = incomeStatementCalculation.getElementsByTag(link, "loc")
        val locsLookup = locs.associateBy { it.attr(xlink, "label") }
        val calculationArcs = incomeStatementCalculation.getElementsByTag(link, "calculationArc")

        val calculationArcLookup = calculationArcs.groupBy { it.attr(xlink, "from") }

        val itemsLookup = locsLookup.mapValues { (locLabel, loc) ->
            val elementDefinition = loc.attr(xlink, "href")?.let { href ->
                ctx.schemaManager.getElementDefinition(href)
            } ?: error("Unable to find element definition name for ${loc.attr(xlink, "href")}")

            //
            // the fragment is actually the id to look up by
            //
            val itemName = elementDefinition.name

            //
            // populate the historical value of the item
            //
            val latestHistoricalFact = ctx.latestFact(itemName)
            val historicalValues = ctx.allHistoricalValues(
                elementName = itemName,
                explicitMembers = latestHistoricalFact?.explicitMembers ?: emptyList()
            )

            val historicalValue = ctx.latestHistoricalValue(itemName)?.value ?: 0.0

            ctx.putElementDefinition(itemName, elementDefinition)

            fun getItemNameFromNode(node: XmlNode): String {
                val to = node.attr(xlink, "to")
                val toHref = locsLookup[to]
                    ?.attr(xlink, "href")
                    ?: error("cannot find loc for $to")

                // the fragment is actually the id to look up by
                return (ctx.schemaManager.getElementDefinition(toHref)
                    ?: error("unable to find a schema definition for $toHref")).name
            }

            val relatedCalcArcs = calculationArcLookup[locLabel]
            val dependentItems = relatedCalcArcs?.map { node -> getItemNameFromNode(node) } ?: emptyList()
            ctx.putDependentItem(itemName, dependentItems)

            val expression = relatedCalcArcs
                ?.joinToString("+") { node ->
                    val dependentItemName = getItemNameFromNode(node)
                    // we must look up the element definition to get it's name in the instance file
                    val weight = node.attr("weight")
                    "$weight*$dependentItemName"
                }
            // if there are no calculation arcs, leave the amount to be the most recent reported number
                ?: "$historicalValue"

            Item(
                name = itemName,
                description = latestHistoricalFact?.labelTerse,
                historicalValue = historicalValue,
                historicalValues = historicalValues,

                /*
                create formulas based on the calculationArcs define in the calculation linkbase
                further: we should store all the dependencies in a map
                 */
                expression = expression
            )
        }

        //
        // there are duplicates in the calculation arc(s) - this attempts to remove that
        //
        return itemsLookup.values.groupBy { it.name }.map { entry ->
            val duplicatedItems = entry.value
            if (duplicatedItems.size != 1) {
                duplicatedItems.find { it.expression != "0.0" } ?: duplicatedItems.first()
            } else {
                duplicatedItems.first()
            }
        }
    }

}