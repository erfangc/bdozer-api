package com.starburst.starburst.edgar.factbase

import com.starburst.starburst.edgar.dataclasses.ElementDefinition
import com.starburst.starburst.edgar.provider.FilingProvider
import com.starburst.starburst.edgar.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import com.starburst.starburst.models.HistoricalValue
import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.URI

class FactBaseModelBuilder(
    filingProvider: FilingProvider,
    factBase: FactBase
) {

    private val calculationLinkbase = filingProvider.calculationLinkbase()
    private val schemaManager = SchemaManager(filingProvider)
    private val latestNondimensionalFacts = factBase.getLatestNonDimensionalFacts(cik = filingProvider.cik())
    private val facts = factBase.getAllFactsForCik(cik = filingProvider.cik())

    fun buildModel(): Model {
        /*
        high levels overview
         */

        /*
        step 1 - start with the calculation XML since
        from there we derive the structure of the `Model` to be created
        everything else serves as a reference (including "XBRL facts" such as actual historical values)
        to the business logic expressed within this page
         */
        val calculationLinks = calculationLinkbase.getElementsByTagNameSafe("link:calculationLink")

        /*
        find the income statement calculation
         */
        val incomeStatementRole = findIncomeStatementRole(calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val incomeStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, incomeStatementRole))

        /*
        find the income statement calculation
         */
        val balanceSheetRole = findBalanceSheetRole(calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val balanceSheetItems = linkCalculationToItems(findLinkCalculationByRole(calculationLinks, balanceSheetRole))

        /*
        find the cash flow statement calculation
         */
        val cashFlowStatementRole =
            findCashFlowStatementRole(calculationLinkbase.getElementsByTagNameSafe("link:roleRef"))
        val cashFlowStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, cashFlowStatementRole))

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = latestNondimensionalFacts["dei:EntityRegistrantName"]
        val symbol = latestNondimensionalFacts["dei:TradingSymbol"]

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

    private fun linkCalculationToItems(incomeStatementCalculation: Node): List<Item> {
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
            val elementDefinition = schemaManager.getElementDefinition(href)
            val name = elementDefinition?.name ?: error("Unable to find element definition name for $href")

            //
            // populate the historical value of the item
            //
            val historicalValue = latestHistoricalValue(elementDefinition)

            //
            // populate all historical values
            //
            val historicalValues = historicalValues(elementDefinition)

            Item(
                name = name,
                historicalValue = historicalValue ?: 0.0,
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
                        val elementName = (schemaManager.getElementDefinition(toHref)
                            ?: error("unable to find a schema definition for $toHref")).name

                        // we must look up the element definition to get it's name in the instance file
                        val weight = node.attributes.getNamedItem("weight").textContent
                        "$weight*$elementName"
                    } ?: "0.0"
            )
        }

        //
        // TODO investigate why there are duplicates
        //
        return itemsLookup.values.groupBy { it.name }.map { entry ->
            if (entry.value.size != 1) {
                entry.value.first { it.expression != "0.0" }
            } else {
                entry.value.first()
            }
        }
    }

    private fun historicalValues(elementDefinition: ElementDefinition): List<HistoricalValue> {
        val filteredFacts = facts
            .filter {
                it.explicitMembers.isEmpty()
                        && elementDefinition.name == it.rawNodeName
                        && it.formType == "10-K" // TODO decide this based on some intelligence
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

    private fun latestHistoricalValue(elementDefinition: ElementDefinition): Double? {
        val nodeName = elementDefinition.name
        return latestNondimensionalFacts[nodeName]?.doubleValue
    }
}