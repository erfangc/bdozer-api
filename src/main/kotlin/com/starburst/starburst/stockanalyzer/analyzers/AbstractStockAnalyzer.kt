package com.starburst.starburst.stockanalyzer.analyzers

import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.edgar.factbase.dataclasses.Calculation
import com.starburst.starburst.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.starburst.starburst.edgar.factbase.dataclasses.Fact
import com.starburst.starburst.edgar.factbase.support.FilingConceptsHolder
import com.starburst.starburst.edgar.factbase.support.LabelManager
import com.starburst.starburst.extensions.DoubleExtensions.orZero
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
import com.starburst.starburst.stockanalyzer.analyzers.extensions.ConceptToItemHelper.conceptHrefToItemName
import com.starburst.starburst.stockanalyzer.analyzers.extensions.ConceptToItemHelper.conceptLabel
import com.starburst.starburst.stockanalyzer.analyzers.extensions.ConceptToItemHelper.expression
import com.starburst.starburst.stockanalyzer.analyzers.extensions.ConceptToItemHelper.historicalValue
import com.starburst.starburst.stockanalyzer.analyzers.extensions.DetermineItemType.isCostOperatingCost
import com.starburst.starburst.stockanalyzer.analyzers.extensions.DetermineItemType.isEpsItem
import com.starburst.starburst.stockanalyzer.analyzers.extensions.DetermineItemType.isOneTime
import com.starburst.starburst.stockanalyzer.analyzers.extensions.DetermineItemType.isTaxItem
import com.starburst.starburst.stockanalyzer.analyzers.extensions.FrequentlyUsedItemFormulaLogic.fillEpsItem
import com.starburst.starburst.stockanalyzer.analyzers.extensions.FrequentlyUsedItemFormulaLogic.fillOneTimeItem
import com.starburst.starburst.stockanalyzer.analyzers.extensions.FrequentlyUsedItemFormulaLogic.fillTaxItem
import com.starburst.starburst.stockanalyzer.analyzers.extensions.General.conceptNotFound
import com.starburst.starburst.stockanalyzer.analyzers.extensions.General.fragment
import com.starburst.starburst.stockanalyzer.analyzers.extensions.PostEvaluationAnalysis.postModelEvaluationAnalysis
import com.starburst.starburst.stockanalyzer.analyzers.extensions.PostEvaluationAnalysis.zeroRevenueGrowth
import com.starburst.starburst.edgar.factbase.dataclasses.Dimension
import com.starburst.starburst.stockanalyzer.dataclasses.StockAnalysis2
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

abstract class AbstractStockAnalyzer(
    dataProvider: StockAnalyzerDataProvider,
    val originalStockAnalysis: StockAnalysis2,
) {

    val filingProvider = dataProvider.filingProvider
    val cik = filingProvider.cik().padStart(10, '0')

    /*
    services
     */
    val executor = Executors.newCachedThreadPool()
    val zacksEstimatesService = dataProvider.zacksEstimatesService
    val filingEntity = dataProvider.filingEntity
    val factBase = dataProvider.factBase
    val conceptManager = FilingConceptsHolder(filingProvider)
    val labelManager = LabelManager(filingProvider)
    val alphaVantageService = dataProvider.alphaVantageService
    val evaluator = ModelEvaluator()

    /*
    analysis properties
     */
    val calculations = factBase.calculations(cik)
    val conceptDependencies = conceptDependencies()

    /*
    parse and store dimensions declared in the income statement
    so they can be applied if concepts needs to be broken down
    into the canonical dimensions represented by the income statement
     */
    val dimensions: List<Dimension>
    init {
        val incomeStatement = calculations.incomeStatement
        /*
        dimensional arcs are any thing in the StatementTable
        immediate child that is not an statement item
         */
        val dimensionsArcs = incomeStatement
            .filter {
                it.parentHref?.endsWith("StatementTable") == true
                        && !it.conceptHref.endsWith("StatementLineItems")
            }
        val instanceDocument = filingProvider.instanceDocument()

        /*
        we turn these dimensional "arcs" declared in the income statements
        into [Dimension] objects - realizing they are represented as a two layer
        graph, from dimension -> domain (which is a bit of a pointless wrapper) -> dimension members
         */
        dimensions = dimensionsArcs.map { dimension ->
            val dimensionHref = dimension.conceptHref
            val dimensionConcept = conceptManager.getConcept(dimensionHref) ?: error("$dimensionHref cannot be found")
            val domainMembers = incomeStatement.filter { it.parentHref == dimensionHref }

            val memberConcepts = domainMembers.flatMap { domainHref ->
                val domainHref = conceptManager.getConcept(domainHref.conceptHref)?.conceptHref
                    ?: error("${domainHref.conceptHref} cannot be found")

                incomeStatement.filter { it.parentHref == domainHref }.map { dimensionMember ->
                    val concept = conceptManager.getConcept(dimensionMember.conceptHref)
                        ?: error("${dimensionMember.conceptHref} cannot be found")
                    val ns = instanceDocument.getShortNamespace(concept.targetNamespace)
                        ?: error("targetNamespace ${concept.targetNamespace} cannot be found")
                    "$ns:${concept.conceptName}"
                }
            }.toSet()

            val ns = instanceDocument.getShortNamespace(dimensionConcept.targetNamespace)
                ?: error("targetnamespace ${dimensionConcept.targetNamespace} cannot be found")

            Dimension(
                dimensionConcept = "$ns:${dimensionConcept.conceptName}",
                memberConcepts = memberConcepts,
            )
        }
    }

    /*
    Concept names
     */
    val totalRevenueConceptName = calculations.conceptNames.totalRevenue ?: error("totalRevenue conceptName cannot be found")
    val epsConceptName = calculations.conceptNames.eps
    val netIncomeConceptName = calculations.conceptNames.netIncome
    val ebitConceptName = calculations.conceptNames.ebit
    val operatingCostConceptName = calculations.conceptNames.operatingCost
    val sharesOutstandingConceptName = calculations.conceptNames.sharesOutstanding ?: error("sharesOutstanding conceptName cannot be found")

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
        val sharesOutstanding = historicalValue(sharesOutstandingConceptName)
        val additionalMandatoryItems = listOf(
            Item(
                name = sharesOutstandingConceptName,
                historicalValue = sharesOutstanding,
                formula = sharesOutstanding?.value.toString(),
            ),
        ).filter { mandatoryItem -> !model.incomeStatementItems.any { item -> item.name == mandatoryItem.name } }

        return listOf(
            Item(
                name = DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = TerminalValuePerShare,
                formula = "if(period=$periods,$epsConceptName * $terminalPeMultiple,0.0)"
            ),
            Item(
                name = PresentValueOfTerminalValuePerShare,
                formula = "$DiscountFactor * $TerminalValuePerShare"
            ),
            Item(
                name = PresentValueOfEarningsPerShare,
                formula = "$DiscountFactor * $epsConceptName"
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
        // TODO if this is empty then aggregate it up from Dimensions
        return factBase
            .getFacts(cik, periodFocus, conceptName)
            .filter { it.explicitMembers.isEmpty() }
            .toList()
    }

    private val log = LoggerFactory.getLogger(AbstractStockAnalyzer::class.java)

    fun analyze(): StockAnalysis2 {

        val model = originalStockAnalysis
            .model
            .copy(incomeStatementItems = createIncomeStatementItems())
        log.info("Finished building income statement items for ${originalStockAnalysis.cik}, building other items")

        val finalModel = model.copy(otherItems = dcfItems(model))
        log.info("Finished building model for ${originalStockAnalysis.cik}, evaluating")

        /*
        Run these in parallel to take advantage of parallelism
         */
        val evaluateModelResult = executor.submit(Callable { evaluator.evaluate(finalModel) })
        val zeroGrowthResult = executor.submit(Callable { evaluator.evaluate(zeroRevenueGrowth(model)) })
        log.info("Finished evaluating model for ${originalStockAnalysis.cik}, running post evaluation analysis")

        val ret = postModelEvaluationAnalysis(evaluateModelResult, zeroGrowthResult)
        log.info("Finished post evaluation analysis for ${originalStockAnalysis.cik}")

        return ret

    }

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
                        subtotal = true,
                    )
                }
                fillInItem(item = item)
            }
    }

    /**
     * This method fills in the given [Item] with the correct formula
     */
    fun fillInItem(item: Item): Item {
        /*
        If this item has an override, use that instead
         */
        val override = originalStockAnalysis.model.itemOverrides.find { it.name == item.name }
        if (override != null) {
            return override
        } else {
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
    }

    abstract fun processOperatingCostItem(item: Item): Item

    protected fun useZacksRevenueEstimate(item: Item): Item {
        val model = originalStockAnalysis.model
        val ticker = filingEntity.tradingSymbol ?: error("No trading symbol defined for $cik")
        val projections = zacksEstimatesService.revenueProjections(ticker = ticker)
        /*
        if there are more periods than there are estimates decide what to do
         */
        val lastDocumentPeriodEnd = item.historicalValue?.documentPeriodEndDate
        val formulas = (1..model.periods).associateWith { period ->
            val finalRevenue = projections[projections.toSortedMap().lastKey()].orZero()
            val year = period + LocalDate.parse(lastDocumentPeriodEnd).year
            (projections[year] ?: finalRevenue).toString()
        }
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
