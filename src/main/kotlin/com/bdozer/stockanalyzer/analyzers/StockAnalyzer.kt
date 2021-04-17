package com.bdozer.stockanalyzer.analyzers

import com.bdozer.edgar.factbase.dataclasses.Calculation
import com.bdozer.extensions.DoubleExtensions.orZero
import com.bdozer.models.ModelEvaluator
import com.bdozer.models.Utility.DiscountFactor
import com.bdozer.models.Utility.PresentValueOfEarningsPerShare
import com.bdozer.models.Utility.PresentValueOfTerminalValuePerShare
import com.bdozer.models.Utility.PresentValuePerShare
import com.bdozer.models.Utility.TerminalValuePerShare
import com.bdozer.models.dataclasses.Discrete
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.models.dataclasses.Model
import com.bdozer.stockanalyzer.analyzers.extensions.PostEvaluationAnalysis.runDerivedAnalytics
import com.bdozer.stockanalyzer.analyzers.extensions.PostEvaluationAnalysis.zeroRevenueGrowth
import com.bdozer.stockanalyzer.analyzers.support.StockAnalyzerDataProvider
import com.bdozer.stockanalyzer.analyzers.support.itemgenerator.ItemGenerator
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class StockAnalyzer(
    dataProvider: StockAnalyzerDataProvider,
    val originalAnalysis: StockAnalysis2,
) {
    private val log = LoggerFactory.getLogger(StockAnalyzer::class.java)
    val filingProvider = dataProvider.filingProvider
    val cik = filingProvider.cik().padStart(10, '0')

    init {
        log.info("Running stock analysis for $cik adsh=${filingProvider.adsh()}")
    }

    /*
    services
     */
    private val executor = Executors.newCachedThreadPool()
    private val zacksEstimatesService = dataProvider.zacksEstimatesService
    val filingEntity = dataProvider.filingEntity
    val factBase = dataProvider.factBase
    val conceptManager = filingProvider.conceptManager()
    val filingCalculationsParser = filingProvider.filingCalculationsParser()
    val calculations = filingCalculationsParser.parseCalculations()
    val alphaVantageService = dataProvider.alphaVantageService
    private val evaluator = ModelEvaluator()

    private val generatedItems = ItemGenerator(filingProvider).generateItems()

    val totalRevenueItemName = generatedItems.revenue?.name

    val sharesOutstandingItem =
        generatedItems.basicAndDilutedSharesOutstanding
            ?: generatedItems.dilutedSharesOutstanding
            ?: generatedItems.basicSharesOutstanding

    val epsItemName = generatedItems.epsBasicAndDiluted?.name
        ?: generatedItems.epsDiluted?.name
        ?: generatedItems.epsBasic?.name

    val netIncomeItemName = generatedItems.netIncome?.name

    val conceptDependencies = conceptDependencies()

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

        val immediateChildrenLookup = (calculations.incomeStatement)
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

    init {
        log.info("Finished initializing the stock analyzer for $cik adsh=${filingProvider.adsh()}")
    }

    /**
     * Create items that would end up computing the NPV of the investment
     */
    private fun dcfItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        val terminalPeMultiple = 1.0 / (discountRate - model.terminalGrowthRate)

        return listOf(
            Item(
                name = DiscountFactor,
                formula = "1 / (1.0 + $discountRate)^period",
            ),
            Item(
                name = TerminalValuePerShare,
                formula = "if(period=$periods,$epsItemName * $terminalPeMultiple,0.0)",
            ),
            Item(
                name = PresentValueOfTerminalValuePerShare,
                formula = "$DiscountFactor * $TerminalValuePerShare",
            ),
            Item(
                name = PresentValueOfEarningsPerShare,
                formula = "$DiscountFactor * $epsItemName",
            ),
            Item(
                name = PresentValuePerShare,
                formula = "$PresentValueOfEarningsPerShare + $PresentValueOfTerminalValuePerShare",
            )
        )
    }

    fun analyze(): StockAnalysis2 {

        val originalModel = originalAnalysis.model.copy(
            totalRevenueConceptName = totalRevenueItemName,
            epsConceptName = epsItemName,
            netIncomeConceptName = netIncomeItemName,
            sharesOutstandingConceptName = sharesOutstandingItem?.name,
        )

        val modelWithGeneratedItems = originalModel.copy(
            incomeStatementItems = generatedItems.incomeStatementItems,
            balanceSheetItems = generatedItems.balanceSheetItems,
        )

        val autoGeneratedModel = modelWithGeneratedItems
            .copy(
                incomeStatementItems = useZacksRevenueEstimate(modelWithGeneratedItems.incomeStatementItems),
                otherItems = dcfItems(modelWithGeneratedItems),
            )

        /*
        add user override
         */
        val readyForEvaluationModel = autoGeneratedModel.copy(
            incomeStatementItems = overlayWithOverrides(
                autoGeneratedModel.incomeStatementItems,
                autoGeneratedModel.itemOverrides
            ),
            balanceSheetItems = overlayWithOverrides(
                autoGeneratedModel.incomeStatementItems,
                autoGeneratedModel.itemOverrides
            ),
        )

        log.info("Finished building model for ${originalAnalysis.cik}, evaluating")

        /*
        Run these in parallel to take advantage of parallelism
         */
        val f1 = executor.submit(Callable { evaluator.evaluate(readyForEvaluationModel) })
        val f2 = executor.submit(Callable { evaluator.evaluate(zeroRevenueGrowth(readyForEvaluationModel)) })
        val evaluateModelResult = f1.get()
        val zeroGrowthResult = f2.get()
        log.info("Finished evaluating model for ${originalAnalysis.cik}, running post evaluation analysis")

        val derivedStockAnalytics = runDerivedAnalytics(evaluateModelResult, zeroGrowthResult)
        log.info("Finished post evaluation analysis for ${originalAnalysis.cik}")

        return originalAnalysis.copy(
            model = autoGeneratedModel,
            lastUpdated = Instant.now(),
            cells = evaluateModelResult.cells,
            derivedStockAnalytics = derivedStockAnalytics,
        )

    }

    private fun overlayWithOverrides(originalItems: List<Item>, itemOverrides: List<Item>): List<Item> {
        val lookup = itemOverrides.associateBy { it.name }
        return originalItems.map { item ->
            if (lookup[item.name] != null) {
                lookup[item.name]!!
            } else {
                item
            }
        }
    }

    private fun useZacksRevenueEstimate(items: List<Item>): List<Item> {
        return items.map { item ->
            if (item.name != totalRevenueItemName) {
                item
            } else {
                val model = originalAnalysis.model
                val ticker = filingEntity.tradingSymbol ?: error("No trading symbol defined for $cik")
                val projections = zacksEstimatesService.revenueProjections(ticker = ticker)
                /*
                if there are more periods than there are estimates decide what to do
                 */
                val lastDocumentPeriodEnd = item.historicalValue?.documentPeriodEndDate
                if (lastDocumentPeriodEnd == null) {
                    log.info("Unable to determine Zacks estimates for revenue item ${item.name}")
                    item
                } else {
                    val formulas = (1..model.periods).associateWith { period ->
                        val finalRevenue = projections[projections.toSortedMap().lastKey()].orZero()
                        val year = period + LocalDate.parse(lastDocumentPeriodEnd).year
                        (projections[year] ?: finalRevenue).toString()
                    }
                    item.copy(
                        type = ItemType.Discrete,
                        discrete = Discrete(formulas)
                    )
                }
            }
        }
    }

}
