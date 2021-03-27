package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.support.FilingConceptsHolder
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.modelbuilder.common.FrequentlyUsedItemFormulaLogic.processEpsItem
import com.starburst.starburst.modelbuilder.common.FrequentlyUsedItemFormulaLogic.processOneTimeItem
import com.starburst.starburst.modelbuilder.common.FrequentlyUsedItemFormulaLogic.processTaxItem
import com.starburst.starburst.modelbuilder.common.GeneralExtensions.fragment
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelperExtensions.conceptHrefToItemName
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelperExtensions.historicalValue
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemTypeExtensions.isCostOperatingCost
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemTypeExtensions.isEpsItem
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemTypeExtensions.isOneTime
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemTypeExtensions.isTaxItem
import com.starburst.starburst.modelbuilder.common.extensions.StockAnalysisExtensions.postModelEvaluationAnalysis
import com.starburst.starburst.modelbuilder.dataclasses.StockAnalysis
import com.starburst.starburst.modelbuilder.templates.EarningsRecoveryAnalyzer
import com.starburst.starburst.models.ModelEvaluator
import com.starburst.starburst.models.Utility.DiscountFactor
import com.starburst.starburst.models.Utility.PresentValueOfEarningsPerShare
import com.starburst.starburst.models.Utility.PresentValueOfTerminalValuePerShare
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.Utility.TerminalValuePerShare
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.HashSet

abstract class AbstractStockAnalyzer(
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val filingEntity: FilingEntity
) {

    val cik = filingProvider.cik().padStart(10, '0')
    val conceptManager = FilingConceptsHolder(filingProvider)
    val labelManager = LabelManager(filingProvider)
    val evaluator = ModelEvaluator()
    val calculations = factBase.calculations(cik)

    val totalRevenueConceptName = totalRevenueItemName()
    val conceptDependencies: Map<String, Set<Calculation>>
    val tradingSymbol = filingEntity.tradingSymbol

    init {
        conceptDependencies = conceptDependencies()
    }

    val log = LoggerFactory.getLogger(EarningsRecoveryAnalyzer::class.java)

    fun timeSeriesVsRevenue(
        conceptName: String,
        periodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<Pair<Double, Double>> {
        val revenueFacts = timeSeries(conceptName = totalRevenueConceptName, periodFocus = periodFocus)
        val otherFacts = timeSeries(
            conceptName = conceptName,
            periodFocus = periodFocus
        ).associateBy { it.documentPeriodEndDate }
        return revenueFacts.sortedBy { it.documentPeriodEndDate }.map { revenue ->
            val otherFact = otherFacts[revenue.documentPeriodEndDate]?.doubleValue ?: 0.0
            (revenue.doubleValue ?: 0.0) to otherFact
        }
    }

    fun dcfItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalGrowthRate)
        return listOf(
            Item(
                name = DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = TerminalValuePerShare,
                formula = "if(period=$periods,${EarningsPerShareDiluted} * ${terminalPeMultiple},0.0)"
            ),
            Item(
                name = PresentValueOfTerminalValuePerShare,
                formula = "$DiscountFactor * $TerminalValuePerShare"
            ),
            Item(
                name = PresentValueOfEarningsPerShare,
                formula = "$DiscountFactor * $EarningsPerShareDiluted"
            ),
            Item(
                name = PresentValuePerShare,
                formula = "$PresentValueOfEarningsPerShare + $PresentValueOfTerminalValuePerShare"
            )
        )
    }

    /**
     * Retrieves from [FactBase] the time series for the given concept
     * (empty of any dimensions) for the specified periodFocus type
     */
    fun timeSeries(
        conceptName: String,
        periodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<Fact> {
        return factBase
            .getFacts(cik, periodFocus, conceptName)
            .filter { it.explicitMembers.isEmpty() }
            .toList()
    }

    fun analyze(): StockAnalysis {
        val model = emptyModel().copy(
            incomeStatementItems = createIncomeStatementItems()
        )
        val finalModel = model.copy(otherItems = dcfItems(model))
        val evalResult = evaluator.evaluate(finalModel)
        return postModelEvaluationAnalysis(evalResult)
    }

    fun emptyModel() = Model(
        name = filingEntity.name,
        symbol = tradingSymbol,
        description = filingEntity.description,
        beta = 1.86,
        terminalGrowthRate = 0.02
    )

    fun createIncomeStatementItems(): List<Item> {
        val incomeStatementArcs = calculations.incomeStatement
        val lineItemsIdx = incomeStatementArcs.indexOfFirst {
            it.conceptHref.fragment() == "us-gaap_StatementLineItems"
        }
        val statementArcs = incomeStatementArcs.subList(
            lineItemsIdx + 1,
            incomeStatementArcs.size
        )

        /*
        Turn each Arc in the statement into an Item based, based on the possibilities:

        - The arc defines calculation components
         */
        return statementArcs
            .map { arc ->
                val conceptHref = arc.conceptHref
                val concept = conceptManager.getConcept(conceptHref) ?: conceptNotFound(conceptHref)
                val historicalValue = historicalValue(concept)

                /**
                 * Find the label of an [Arc]
                 */
                fun arcLabel(arc: Arc): String {
                    val label = labelManager.getLabel(arc.conceptHref.fragment())
                    return label?.terseLabel ?: label?.label ?: arc.conceptName
                }

                /**
                 * For an Arc with dependent calculations
                 * create a String based formula based on the weight
                 * and concept defined in those calculations
                 */
                fun expression(arc: Arc): String {
                    val positives = arc
                        .calculations
                        .filter { it.weight > 0 }
                        .joinToString("+") { conceptHrefToItemName(it.conceptHref) }

                    val negatives = arc
                        .calculations
                        .filter { it.weight < 0 }
                        .joinToString("-") { conceptHrefToItemName(it.conceptHref) }

                    return if (negatives.isNotEmpty()) {
                        "$positives - $negatives"
                    } else {
                        positives
                    }
                }

                val item = if (arc.calculations.isEmpty()) {
                    Item(
                        name = conceptHrefToItemName(conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        formula = "${historicalValue?.value ?: 0.0}",
                    )
                } else {
                    Item(
                        name = conceptHrefToItemName(conceptHref),
                        description = arcLabel(arc),
                        historicalValue = historicalValue,
                        formula = expression(arc),
                    )
                }
                processItem(item = item)
            }
    }

    fun processItem(item: Item): Item {
        return when {
            item.name == totalRevenueConceptName -> {
                processTotalRevenueItem(item)
            }
            isEpsItem(item) -> {
                processEpsItem(item)
            }
            isTaxItem(item) -> {
                processTaxItem(item)
            }
            isOneTime(item) -> {
                processOneTimeItem(item)
            }
            isCostOperatingCost(item) -> {
                processOperatingCostItem(item)
            }
            else -> {
                item
            }
        }
    }

    abstract fun processOperatingCostItem(item: Item): Item
    abstract fun processTotalRevenueItem(item: Item): Item

    fun totalRevenueItemName(): String {
        return calculations
            .incomeStatement
            .find {
                setOf("RevenueFromContractWithCustomerExcludingAssessedTax").contains(it.conceptName)
            }?.conceptName ?: error("unable to find revenue total item name for $cik")
    }

    /**
     * This method creates a flattened list of dependencies
     * from any concept to all other concepts that roll up into it
     *
     * Therefore if NetIncomeLoss = OperatingIncomeLoss - NonOperatingExpenses
     * and OperatingIncomeLoss = GrossProfit - OperatingExpenses
     *
     * Then, NetIncomeLoss = [GrossProfit, OperatingExpenses, NonOperatingExpenses]
     */
    private fun conceptDependencies(): Map<String, Set<Calculation>> {

        val immediateChildrenLookup = (calculations.incomeStatement
                + calculations.cashFlowStatement
                + calculations.balanceSheet)
            .associateBy { it.conceptName }

        fun flattenSingleConcept(conceptName: String): HashSet<Calculation> {
            val calculations = immediateChildrenLookup[conceptName]?.calculations ?: emptyList()
            val results = HashSet<Calculation>()

            /*
            use a DFS algorithm
             */
            val stack = Stack<Calculation>()
            stack.addAll(calculations)
            while (stack.isNotEmpty()) {
                val calculation = stack.pop()
                val childrenCalculations = immediateChildrenLookup[calculation.conceptName]
                    ?.calculations ?: emptyList()
                if (childrenCalculations.isNotEmpty()) {
                    stack.addAll(childrenCalculations)
                } else {
                    results.add(calculation)
                }
            }
            return results
        }

        return immediateChildrenLookup
            .keys
            .map { key -> key to flattenSingleConcept(key) }
            .filter { it.second.isNotEmpty() }
            .toMap()

    }

}