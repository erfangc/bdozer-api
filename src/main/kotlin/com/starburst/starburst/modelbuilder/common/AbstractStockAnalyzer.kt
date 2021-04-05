package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.DoubleExtensions.orZero
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic
import com.starburst.starburst.edgar.factbase.support.FilingConceptsHolder
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelper.conceptHrefToItemName
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelper.conceptLabel
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelper.expression
import com.starburst.starburst.modelbuilder.common.extensions.ConceptToItemHelper.historicalValue
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemType.isCostOperatingCost
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemType.isEpsItem
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemType.isOneTime
import com.starburst.starburst.modelbuilder.common.extensions.DetermineItemType.isTaxItem
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.ebitItemName
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.fillEpsItem
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.fillOneTimeItem
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.fillTaxItem
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.operatingCostsItemName
import com.starburst.starburst.modelbuilder.common.extensions.FrequentlyUsedItemFormulaLogic.totalRevenueItemName
import com.starburst.starburst.modelbuilder.common.extensions.General.conceptNotFound
import com.starburst.starburst.modelbuilder.common.extensions.General.fragment
import com.starburst.starburst.modelbuilder.common.extensions.PostEvaluationAnalysis.postModelEvaluationAnalysis
import com.starburst.starburst.modelbuilder.dataclasses.StockAnalysis
import com.starburst.starburst.modelbuilder.analyzers.CrashAndRecovery
import com.starburst.starburst.models.ModelEvaluator
import com.starburst.starburst.models.Utility.DiscountFactor
import com.starburst.starburst.models.Utility.PresentValueOfEarningsPerShare
import com.starburst.starburst.models.Utility.PresentValueOfTerminalValuePerShare
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.Utility.TerminalValuePerShare
import com.starburst.starburst.models.dataclasses.Discrete
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.collections.HashSet

abstract class AbstractStockAnalyzer(
    dataProvider: StockAnalyzerDataProvider
) : StockAnalyzer {

    private val log = LoggerFactory.getLogger(CrashAndRecovery::class.java)

    val filingProvider = dataProvider.filingProvider
    val zacksEstimatesService = dataProvider.zacksEstimatesService
    val filingEntity = dataProvider.filingEntity
    val cik = filingProvider.cik().padStart(10, '0')
    val factBase = dataProvider.factBase
    val conceptManager = FilingConceptsHolder(filingProvider)
    val labelManager = LabelManager(filingProvider)
    val evaluator = ModelEvaluator()
    val calculations = factBase.calculations(cik)
    val totalRevenueConceptName = totalRevenueItemName()
    val ebitConceptName = ebitItemName()
    val operatingCostConceptName = operatingCostsItemName()
    val conceptDependencies = conceptDependencies()

    fun timeSeriesVsRevenue(
        conceptName: String,
        periodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<Pair<Double, Double>> {
        val revenueFacts = timeSeries(
            conceptName = totalRevenueConceptName,
            periodFocus = periodFocus
        )
        val otherFacts = timeSeries(
            conceptName = conceptName,
            periodFocus = periodFocus
        ).associateBy { it.documentPeriodEndDate }
        return revenueFacts.sortedBy { it.documentPeriodEndDate }.map { revenue ->
            val otherFact = otherFacts[revenue.documentPeriodEndDate]?.doubleValue ?: 0.0
            (revenue.doubleValue ?: 0.0) to otherFact
        }
    }

    /**
     * Create items that would end up computing the NPV of the investment
     */
    fun dcfItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalGrowthRate)

        /*
        add some mandatory fields if they don't already exist
         */

        val weightedAverageNumberOfDilutedSharesOutstanding =
            historicalValue(WeightedAverageNumberOfDilutedSharesOutstanding)
        val weightedAverageNumberOfSharesOutstandingBasic =
            historicalValue(WeightedAverageNumberOfSharesOutstandingBasic)

        val additionalMandatoryItems = listOf(
            Item(
                name = WeightedAverageNumberOfDilutedSharesOutstanding,
                historicalValue = weightedAverageNumberOfDilutedSharesOutstanding,
                formula = weightedAverageNumberOfDilutedSharesOutstanding?.value.toString()
            ),
            Item(
                name = WeightedAverageNumberOfSharesOutstandingBasic,
                historicalValue = weightedAverageNumberOfSharesOutstandingBasic,
                formula = weightedAverageNumberOfSharesOutstandingBasic?.value.toString()
            )
        ).filter { mandatoryItem -> !model.incomeStatementItems.any { item -> item.name == mandatoryItem.name } }

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
        ) + additionalMandatoryItems
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

    override fun analyze(): StockAnalysis {
        val model = emptyModel()
            .copy(incomeStatementItems = createIncomeStatementItems())
        val finalModel = model.copy(otherItems = dcfItems(model))
        val evalResult = evaluator.evaluate(finalModel)
        return postModelEvaluationAnalysis(evalResult)
    }

    fun emptyModel() = Model(
        name = filingEntity.name,
        symbol = filingEntity.tradingSymbol,
        description = filingEntity.description,
        beta = filingEntity.beta,
        terminalGrowthRate = 0.02,
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

                val item = if (arc.calculations.isEmpty()) {
                    Item(
                        name = conceptHrefToItemName(conceptHref),
                        description = conceptLabel(conceptHref),
                        historicalValue = historicalValue,
                        formula = "${historicalValue?.value ?: 0.0}",
                    )
                } else {
                    Item(
                        name = conceptHrefToItemName(conceptHref),
                        description = conceptLabel(conceptHref),
                        historicalValue = historicalValue,
                        formula = expression(arc),
                    )
                }
                fillInItem(item = item)
            }
    }

    /**
     * This method fills in the given [Item] with the correct formula
     */
    fun fillInItem(item: Item): Item {
        return when {
            /*
            Revenue by default sources from Zack's median estimates
             */
            item.name == totalRevenueConceptName -> {
                useZacksRevenueEstimate(item)
            }
            isEpsItem(item) -> {
                fillEpsItem(item)
            }
            isTaxItem(item) -> {
                fillTaxItem(item)
            }
            isOneTime(item) -> {
                fillOneTimeItem(item)
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

    protected fun useZacksRevenueEstimate(item: Item): Item {
        val model = emptyModel()
        val projections = zacksEstimatesService.revenueProjections(ticker = model.symbol!!)
        /*
        if there are more periods than there are estimates decide what to do
         */
        val lastDocumentPeriodEnd = item.historicalValue?.documentPeriodEndDate
        val formulas = (1..model.periods).map { period ->
            val finalRevenue = projections[projections.toSortedMap().lastKey()].orZero()
            val year = period + LocalDate.parse(lastDocumentPeriodEnd).year
            period to (projections[year] ?: finalRevenue).toString()
        }.toMap()
        return item.copy(
            type = ItemType.Discrete,
            discrete = Discrete(formulas)
        )
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

        val immediateChildrenLookup = (
                calculations.incomeStatement
                )
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