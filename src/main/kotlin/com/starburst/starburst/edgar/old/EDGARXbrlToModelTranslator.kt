package com.starburst.starburst.edgar.old

import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.edgar.dataclasses.XbrlUtils.readXml
import com.starburst.starburst.edgar.utils.ElementExtension.getElementsByTagNameSafe
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URI

/**
 * This is the main class that handles translation of the XBRL file
 * in EDGAR filings into [Model] instances
 */
class EDGARXbrlToModelTranslator(
    usGaapXsdStream: InputStream,
    instanceStream: InputStream,
    extensionXsdStream: InputStream,
    private val calculationStream: InputStream,
    private val labelStream: InputStream,
    private val definitionStream: InputStream,
) {

    private val factFinder = FactFinder(instanceStream)
    private val elementDefinitionFinder = ElementDefinitionFinder(gaapXsd = usGaapXsdStream, extXsd = extensionXsdStream)

    fun translate(): Model {

        /*
        high levels overview
         */

        /*
        step 1 - start with the calculation XML since
        from there we derive the structure of the `Model` to be created
        everything else serves as a reference (including "XBRL facts" such as actual historical values)
        to the business logic expressed within this page
         */
        val root = readXml(calculationStream)
        val calculationLinks = root.getElementsByTagNameSafe("link:calculationLink")

        /*
        find the income statement calculation
         */
        val incomeStatementRole = findIncomeStatementRole(root.getElementsByTagNameSafe("link:roleRef"))
        val incomeStatementItems =
            linkCalculationToItems(findLinkCalculationByRole(calculationLinks, incomeStatementRole))

        /*
        find the income statement calculation
         */
        val balanceSheetRole = findBalanceSheetRole(root.getElementsByTagNameSafe("link:roleRef"))
        val balanceSheetItems = linkCalculationToItems(findLinkCalculationByRole(calculationLinks, balanceSheetRole))

        /*
        find the cash flow statement calculation
         */
        val cashFlowStatementRole = findCashFlowStatementRole(root.getElementsByTagNameSafe("link:roleRef"))
        val cashFlowStatementItems = linkCalculationToItems(findLinkCalculationByRole(calculationLinks, cashFlowStatementRole))

        /*
        as we encounter "location"s we create placeholder for them as Item(s)
        their historical values are resolved via look up against the Instance document
        labels are resolved via the label document
         */
        val name = factFinder.getString("EntityRegistrantName", "dei")
        val symbol = factFinder.getString("TradingSymbol", "dei")
        return Model(
            name = name ?: "",
            symbol = symbol ?: "",
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
            val fragment = URI(href).fragment
            val elementDefinition = elementDefinitionFinder.lookupElementDefinition(fragment)
            val name = elementDefinition.name

            //
            // populate the historical value of the item
            //
            val historicalValue = factFinder.get(name, elementDefinition.namespace)

            Item(
                name = name,
                historicalValue = historicalValue ?: 0.0,
                expression = calculationArcLookup[locLabel]
                    ?.joinToString("+") { node ->
                        val to = node.attributes.getNamedItem("xlink:to").textContent
                        val toHref = locsLookup[to]
                            ?.attributes
                            ?.getNamedItem("xlink:href")
                            ?.textContent
                            ?: error("cannot find loc for $to")

                        // the fragment is actually the id to look up by
                        val elementName = elementDefinitionFinder
                            .lookupElementDefinition(URI(toHref).fragment)
                            .name

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


}
