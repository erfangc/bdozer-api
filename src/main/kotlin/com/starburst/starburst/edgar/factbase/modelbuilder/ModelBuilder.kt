package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.xml.XmlNode
import org.springframework.stereotype.Service
import java.net.URI

@Service(value = "factBaseModelBuilder")
class ModelBuilder(
    private val filingProviderFactory: FilingProviderFactory,
    private val factBase: FactBase
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

        return Model(
            name = name,
            symbol = symbol,
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = cashFlowStatementItems,
            otherItems = emptyList(),
        )
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
            val elementName = elementDefinition.name

            //
            // populate the historical value of the item
            //
            val latestHistoricalFact = ctx.latestFact(elementName)
            val historicalValues = ctx.allHistoricalValues(
                elementName = elementName,
                explicitMembers = latestHistoricalFact?.explicitMembers ?: emptyList()
            )

            Item(
                name = elementName,
                description = latestHistoricalFact?.labelTerse,
                historicalValue = ctx.latestHistoricalValue(elementName)?.value ?: 0.0,
                historicalValues = historicalValues,
                expression = calculationArcLookup[locLabel]
                    ?.joinToString("+") { node ->
                        val to = node.attr(xlink, "to")
                        val toHref = locsLookup[to]
                            ?.attr(xlink, "href")
                            ?: error("cannot find loc for $to")

                        // the fragment is actually the id to look up by
                        val dependentElementName = (ctx.schemaManager.getElementDefinition(toHref)
                            ?: error("unable to find a schema definition for $toHref")).name

                        // we must look up the element definition to get it's name in the instance file
                        val weight = node.attributes.getNamedItem("weight").textContent
                        "$weight*$dependentElementName"
                    } ?: "0.0"
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