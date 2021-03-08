package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.edgar.XmlElement
import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.support.SchemaManager
import com.starburst.starburst.edgar.provider.FilingProviderFactory
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import org.springframework.stereotype.Service
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.URI

@Service(value = "factBaseModelBuilder")
class ModelBuilder(
    private val filingProviderFactory: FilingProviderFactory,
    private val factBase: FactBase
) {

    internal data class ModelBuilderContext(
        val calculationLinkbase: XmlElement,
        val schemaManager: SchemaManager,
        val latestNonDimensionalFacts: Map<String, Fact>,
        val facts: List<Fact>
    )

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
        val calculationLinks = ctx.calculationLinkbase.getElementsByTagNameSafe("link:calculationLink")

        /*
        find the income statement calculation
         */
        val incomeStatementRole =
            findIncomeStatementRole(ctx.calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val incomeStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, incomeStatementRole), ctx)

        /*
        find the income statement calculation
         */
        val balanceSheetRole = findBalanceSheetRole(ctx.calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val balanceSheetItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, balanceSheetRole), ctx)

        /*
        find the cash flow statement calculation
         */
        val cashFlowStatementRole =
            findCashFlowStatementRole(ctx.calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val cashFlowStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, cashFlowStatementRole), ctx)

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = ctx.latestNonDimensionalFacts["EntityRegistrantName"]
        val symbol = ctx.latestNonDimensionalFacts["TradingSymbol"]

        return Model(
            name = name?.stringValue ?: "",
            symbol = symbol?.stringValue ?: "",
            tags = emptyList(),
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = cashFlowStatementItems,
            otherItems = emptyList(),
        )
    }

    private fun findCashFlowStatementRole(nodes: NodeList): String {
        return nodes.toList().first {
            val roleURI = it.attributes.getNamedItem("roleURI").textContent
            val last = URI(roleURI)
                .path
                .split("/")
                .last()
                .toLowerCase()
            (last.contains("statement") || last.contains("consolidated"))
                    && (
                    (last.contains("cash") && last.contains("flow"))
                    )

        }.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }


    private fun buildContext(cik: String, adsh: String): ModelBuilderContext {
        val filingProvider = filingProviderFactory.createFilingProvider(cik, adsh)
        return ModelBuilderContext(
            calculationLinkbase = filingProvider.calculationLinkbase(),
            schemaManager = SchemaManager(filingProvider),
            latestNonDimensionalFacts = factBase.latestNonDimensionalFacts(cik = filingProvider.cik()),
            facts = factBase.allFactsForCik(cik = filingProvider.cik())
        )
    }

    private fun findBalanceSheetRole(nodes: NodeList?): String {
        return nodes?.toList()?.first {
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
        }?.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find balance sheet role")
    }

    private fun findIncomeStatementRole(nodes: NodeList?): String {
        return nodes?.toList()?.first {
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
        }?.attributes?.getNamedItem("roleURI")?.textContent ?: error("unable to find income statement role")
    }

    private fun findLinkCalculationByRole(calculationLinks: NodeList, role: String): Node {
        val list = calculationLinks.toList()
        return list
            .find {
                it.attributes.getNamedItem("xlink:role").textContent == role
            }
            ?: error("cannot find $role")
    }

    private fun linkCalculationToItems(incomeStatementCalculation: Node, ctx: ModelBuilderContext): List<Item> {
        //
        // to build the income statement, first find all the loc elements
        //
        val locs = incomeStatementCalculation.childNodes.toList().filter {
            it.nodeName == "link:loc" || it.nodeName == "loc"
        }
        val locsLookup = locs.associateBy { it.attributes.getNamedItem("xlink:label").textContent }
        val calculationArcs = incomeStatementCalculation
            .childNodes
            .toList().filter {
                it.nodeName == "link:calculationArc" || it.nodeName == "calculationArc"
            }
        val calculationArcLookup = calculationArcs
            .groupBy { it.attributes.getNamedItem("xlink:from").textContent }

        val itemsLookup = locsLookup.mapValues { (locLabel, loc) ->
            val href = loc.attributes.getNamedItem("xlink:href").textContent

            //
            // the fragment is actually the id to look up by
            //
            val elementDefinition = ctx.schemaManager.getElementDefinition(href)
            val name = elementDefinition?.name ?: error("Unable to find element definition name for $href")

            //
            // populate the historical value of the item
            //
            val latestHistoricalFact = latestHistoricalValue(elementDefinition, ctx)

            //
            // populate all historical values
            //
            val historicalValues = historicalValues(elementDefinition, ctx)

            Item(
                name = name,
                description = latestHistoricalFact?.labelTerse,
                historicalValue = latestHistoricalFact?.doubleValue ?: 0.0,
                historicalValues = historicalValues,
                expression = calculationArcLookup[locLabel]
                    ?.joinToString("+") { node ->
                        val to = node.attributes.getNamedItem("xlink:to").textContent
                        val toHref = locsLookup[to]
                            ?.attributes
                            ?.getNamedItem("xlink:href")
                            ?.textContent
                            ?: error("cannot find loc for $to")

                        // the fragment is actually the id to look up by
                        val elementName = (ctx.schemaManager.getElementDefinition(toHref)
                            ?: error("unable to find a schema definition for $toHref")).name

                        // we must look up the element definition to get it's name in the instance file
                        val weight = node.attributes.getNamedItem("weight").textContent
                        "$weight*$elementName"
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

    private fun historicalValues(
        elementDefinition: ElementDefinition,
        ctx: ModelBuilderContext
    ): List<HistoricalValue> {
        val filteredFacts = ctx.facts
            .filter {
                        elementDefinition.name == it.elementName
                        && it.formType == "10-K"
            }

        //
        // now decide what is more relevant: Rolling LTM or Rolling YoY or Quarterly
        //
        return filteredFacts.map { fact ->
            HistoricalValue(
                factId = fact._id,
                value = fact.doubleValue,
                startDate = fact.period.startDate?.toString(),
                endDate = fact.period.endDate?.toString(),
                instant = fact.period.instant?.toString()
            )
        }
    }

    private fun latestHistoricalValue(elementDefinition: ElementDefinition, ctx: ModelBuilderContext): Fact? {
        val nodeName = elementDefinition.name
        return ctx.latestNonDimensionalFacts[nodeName]
    }
}