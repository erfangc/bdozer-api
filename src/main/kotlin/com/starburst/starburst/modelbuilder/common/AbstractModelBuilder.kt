package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.edgar.FilingProvider
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.ingestor.dataclasses.Arc
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareBasic
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.EarningsPerShareDiluted
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.NetIncomeLoss
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfDilutedSharesOutstanding
import com.starburst.starburst.edgar.factbase.modelbuilder.formula.USGaapConstants.WeightedAverageNumberOfSharesOutstandingBasic
import com.starburst.starburst.edgar.factbase.support.ConceptManager
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.modelbuilder.common.Extensions.fragment
import com.starburst.starburst.modelbuilder.templates.EarningsRecoveryAnalyzer
import com.starburst.starburst.models.ModelEvaluator
import com.starburst.starburst.models.Utility.DiscountFactor
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.Utility.TerminalValuePerShare
import com.starburst.starburst.models.dataclasses.HistoricalValue
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.ItemType
import com.starburst.starburst.models.dataclasses.Model
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashSet

abstract class AbstractModelBuilder(
    val filingProvider: FilingProvider,
    val factBase: FactBase,
    val filingEntity: FilingEntity,
) {

    val executor = Executors.newCachedThreadPool()
    val cik = filingProvider.cik().padStart(10, '0')
    val conceptManager = ConceptManager(filingProvider)
    val labelManager = LabelManager(filingProvider)
    val evaluator = ModelEvaluator()
    val calculations = factBase.calculations(cik)

    val revenueConceptName = revenueItem()
    val conceptDependencies: Map<String, Set<Calculation>>
    val incomeStatement = calculations.incomeStatement
    val tradingSymbol = filingEntity.tradingSymbol

    init {
        conceptDependencies = conceptDependencies()
    }

    val log = LoggerFactory.getLogger(EarningsRecoveryAnalyzer::class.java)

    protected fun timeSeriesVsRevenue(
        conceptName: String,
        periodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<Pair<Double, Double>> {
        val revenueFacts = timeSeries(conceptName = revenueConceptName, periodFocus = periodFocus)
        val otherFacts = timeSeries(
            conceptName = conceptName,
            periodFocus = periodFocus
        ).associateBy { it.documentPeriodEndDate }
        val arr = revenueFacts.sortedBy { it.documentPeriodEndDate }.map { revenue ->
            val otherFact = otherFacts[revenue.documentPeriodEndDate]?.doubleValue ?: 0.0
            (revenue.doubleValue ?: 0.0) to otherFact
        }
        return arr
    }

    protected fun deriveOtherItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalGrowthRate)
        return listOf(
            Item(
                name = DiscountFactor,
                expression = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = TerminalValuePerShare,
                expression = "if(period=$periods,${EarningsPerShareDiluted} * ${terminalPeMultiple},0.0)"
            ),
            Item(
                name = PresentValuePerShare,
                expression = "$DiscountFactor * (${EarningsPerShareDiluted} + ${TerminalValuePerShare})"
            )
        )
    }

    /**
     * Retrieves from [FactBase] the time series for the given concept
     * (empty of any dimensions) for the specified periodFocus type
     */
    protected fun timeSeries(
        conceptName: String,
        periodFocus: DocumentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
    ): List<Fact> {
        return factBase
            .getFacts(cik, periodFocus, conceptName)
            .filter { it.explicitMembers.isEmpty() }
            .toList()
    }

    /**
     * Determines the "name" of an [Item] based on the
     * href of a concept
     */
    protected fun itemNameFromHref(conceptHref: String): String {
        return conceptManager
            .getConceptDefinition(conceptHref)
            ?.conceptName ?: conceptHref.fragment()
    }

    /**
     * [Item]
     */
    protected fun isOneTime(item: Item): Boolean {
        return setOf("RestructuringAndOtherExpenseIncomeMainline").contains(item.name)
    }

    /**
     * Find the label of an [Arc]
     */
    protected fun arcLabel(arc: Arc): String {
        val label = labelManager.getLabel(arc.conceptHref.fragment())
        return label?.terseLabel ?: label?.label ?: arc.conceptName
    }

    /**
     * Find the [HistoricalValue] of an [Arc] (most recent annual)
     */
    protected fun historicalValue(arc: Arc): HistoricalValue? {
        val conceptName = arc.conceptName
        val fact = factBase.getFacts(
            cik,
            DocumentFiscalPeriodFocus.FY,
            conceptName
        ).find { fact -> fact.explicitMembers.isEmpty() } ?: return null

        return HistoricalValue(
            factId = fact._id,
            documentFiscalPeriodFocus = fact.documentFiscalPeriodFocus.toString(),
            documentPeriodEndDate = fact.documentPeriodEndDate.toString(),
            value = fact.doubleValue,
            startDate = fact.startDate.toString(),
            endDate = fact.endDate.toString(),
            instant = fact.instant.toString(),
        )
    }

    protected fun expression(arc: Arc): String {
        val positives = arc
            .calculations
            .filter { it.weight > 0 }
            .joinToString("+") { itemNameFromHref(it.conceptHref) }

        val negatives = arc
            .calculations
            .filter { it.weight < 0 }
            .joinToString("-") { itemNameFromHref(it.conceptHref) }

        return if (negatives.isNotEmpty()) {
            "$positives - $negatives"
        } else {
            positives
        }
    }

    abstract fun analyze(): StockAnalysis

    protected fun createEpsItem(item: Item): Item {
        return when (item.name) {
            EarningsPerShareDiluted -> {
                item.copy(
                    expression = "$NetIncomeLoss / $WeightedAverageNumberOfDilutedSharesOutstanding"
                )
            }
            EarningsPerShareBasic -> {
                item.copy(
                    expression = "$NetIncomeLoss / $WeightedAverageNumberOfSharesOutstandingBasic"
                )
            }
            else -> {
                item
            }
        }
    }

    private fun revenueItem(): String {
        return calculations
            .incomeStatement
            .find {
                setOf("RevenueFromContractWithCustomerExcludingAssessedTax").contains(it.conceptName)
            }?.conceptName ?: error("unable to find revenue total item name for $cik")
    }

    private fun conceptDependencies(): Map<String, HashSet<Calculation>> {
        val immediateChildrenLookup = calculations
            .incomeStatement
            .associateBy { it.conceptName }

        fun flattenSingleConcept(conceptName: String): HashSet<Calculation> {

            val calculations = immediateChildrenLookup[conceptName]?.calculations ?: emptyList()
            val results = HashSet<Calculation>()
            val stack = Stack<Calculation>()
            stack.addAll(calculations)

            while (stack.isNotEmpty()) {
                val calculation = stack.pop()
                val childrenCalculations = immediateChildrenLookup[calculation.conceptName]?.calculations ?: emptyList()
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

    protected fun isTaxItem(item: Item): Boolean = item.name == USGaapConstants.IncomeTaxExpenseBenefit

    protected fun taxItems(item: Item): Item {
        return item.copy(
            expression = "${USGaapConstants.IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest}*0.12"
        )
    }

    protected fun isEpsItem(item: Item) =
        item.name == EarningsPerShareBasic || item.name == EarningsPerShareDiluted

    protected fun zeroRevenueGrowth(finalModel: Model): Model {
        val updIs = finalModel.incomeStatementItems.map { item ->
            if (item.name == revenueConceptName) {
                item.copy(expression = item.historicalValue?.value.toString(), type = ItemType.Custom)
            } else {
                item
            }
        }
        return finalModel.copy(incomeStatementItems = updIs)
    }


}