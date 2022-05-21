package com.bdozer.api.web.zacksmodels

import com.bdozer.api.models.dataclasses.*
import com.bdozer.api.stockanalysis.master.models.MT
import com.bdozer.api.web.stockanalysis.PostgresService
import com.bdozer.api.web.stockanalysis.StockAnalysisService
import com.bdozer.api.web.stockanalysis.support.zacks.models.FC
import com.bdozer.api.web.stockanalysis.support.zacks.models.SE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ZacksModelBuilder(
    private val stockAnalysisService: StockAnalysisService,
    private val postgresService: PostgresService,
) {

    private val scale = 1000000
    private val log = LoggerFactory.getLogger(ZacksModelBuilder::class.java)

    private fun Double?.or0() = this ?: 0.0

    fun buildZacksModel(ticker: String): BuildZacksModelResponse {
        return try {
            doAnalysis(ticker)
        } catch (e: Exception) {
            BuildZacksModelResponse(
                id = "-",
                ticker = ticker,
                status = 500,
                message = e.message,
            )
        }
    }
    
    private fun doAnalysis(ticker: String): BuildZacksModelResponse {
        fun badInput(message: String): BuildZacksModelResponse {
            return BuildZacksModelResponse(
                id = "-",
                ticker = ticker,
                message = message,
                status = 400
            )
        }

        val ses = ses(ticker)
        val fc = fc(ticker) ?: return badInput("missing entry in fc")
        val mt = mt(ticker) ?: return badInput("missing entry in mt")

        if (ses.isEmpty()) {
            return badInput("missing entries in se")
        }

        log.info("Building Zacks model for cik={} ticker={} ses={}", mt.comp_cik, mt.ticker, ses.size)

        val revenue = Item(
            name = "tot_revnu",
            description = "Revenue",
            type = ItemType.ManualProjections,
            formula = "0.0",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_revnu"),
                value = scale * fc.tot_revnu.or0()
            ),
            manualProjections = ManualProjections(
                manualProjections = ses.map { se ->
                    ManualProjection(
                        fiscalYear = se.per_fisc_year ?: return badInput("se.per_fisc_year cannot be null"),
                        value = se.sales_mean_est.or0() * scale
                    )
                }
            )
        )

        val costOfGoodsSold = Item(
            name = "cost_good_sold",
            description = "Cost of Goods Sold",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("cost_of_goods_sold"),
                value = fc.cost_good_sold.or0() * scale
            ),
            type = ItemType.PercentOfRevenue,
            percentOfRevenue = PercentOfRevenue(
                fc.cost_good_sold.or0() /
                        (fc.tot_revnu ?: return badInput("fc.tot_revnu cannot be null"))
            )
        )

        val grossProfit = Item(
            name = "gross_profit",
            description = "Gross Profit",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("cost_of_goods_sold"),
                value = fc.gross_profit.or0() * scale
            ),
            type = ItemType.SumOfOtherItems,
            sumOfOtherItems = SumOfOtherItems(
                listOf(
                    Component(weight = 1.0, itemName = revenue.name),
                    Component(weight = -1.0, itemName = costOfGoodsSold.name)
                )
            )
        )

        val resDevExp = Item(
            name = "res_dev_exp",
            description = "Research and Development",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("res_dev_exp"),
                value = fc.res_dev_exp.or0() * scale
            ),
            type = ItemType.PercentOfRevenue,
            percentOfRevenue = PercentOfRevenue(fc.res_dev_exp.or0() / fc.tot_revnu)
        )

        val totSellGenAdminExp = Item(
            name = "tot_sell_gen_admin_exp",
            description = "SG&A",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_sell_gen_admin_exp"),
                value = fc.tot_sell_gen_admin_exp.or0() * scale
            ),
            type = ItemType.PercentOfRevenue,
            percentOfRevenue = PercentOfRevenue(fc.tot_sell_gen_admin_exp.or0() / fc.tot_revnu)
        )

        val totDeprecAmort = Item(
            name = "tot_deprec_amort",
            description = "Depreciation and Amortization",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_deprec_amort"),
                value = fc.tot_deprec_amort.or0() * scale
            ),
            type = ItemType.FixedCost,
            fixedCost = FixedCost(cost = scale * fc.tot_deprec_amort.or0())
        )

        val otherOperatingExpense = Item(
            name = "other_operating_expense",
            description = "Other Operating Expense",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("other_operating_expense"),
                value = (
                        fc.int_exp_oper.or0()
                                + fc.int_invst_income_oper.or0()
                                + fc.in_proc_res_dev_exp_aggr.or0()
                                + fc.rental_exp_ind_broker.or0()
                                + fc.pension_post_retire_exp.or0()
                                + fc.other_oper_income_exp.or0()
                        ) * scale
            ),
            type = ItemType.FixedCost,
            fixedCost = FixedCost(cost = 0.0)
        )

        val operIncome = Item(
            name = "oper_income",
            description = "Operating Income",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_deprec_amort"),
                value = (
                        fc.gross_profit.or0()
                                - fc.res_dev_exp.or0()
                                - fc.tot_sell_gen_admin_exp.or0()
                                - fc.tot_deprec_amort.or0()
                        ) * scale
                        - otherOperatingExpense.historicalValue?.value.or0()
            ),
            type = ItemType.SumOfOtherItems,
            sumOfOtherItems = SumOfOtherItems(
                components = listOf(
                    Component(weight = 1.0, itemName = grossProfit.name),
                    Component(weight = -1.0, itemName = resDevExp.name),
                    Component(weight = -1.0, itemName = totSellGenAdminExp.name),
                    Component(weight = -1.0, itemName = totDeprecAmort.name),
                    Component(weight = -1.0, itemName = otherOperatingExpense.name),
                )
            )
        )

        val totNonOperIncomeExp = Item(
            name = "tot_non_oper_income_exp",
            description = "Non Operating Expense",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_non_oper_income_exp"),
                value = fc.tot_non_oper_income_exp.or0() * scale
            ),
            type = ItemType.FixedCost,
            fixedCost = FixedCost(cost = 0.0)
        )

        val preTaxIncome = Item(
            name = "pre_tax_income",
            description = "Non Operating Expense",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("pre_tax_income"),
                value = fc.pre_tax_income.or0() * scale
            ),
            type = ItemType.SumOfOtherItems,
            sumOfOtherItems = SumOfOtherItems(
                components = listOf(
                    Component(weight = 1.0, itemName = operIncome.name),
                    Component(weight = -1.0, itemName = totNonOperIncomeExp.name),
                )
            )
        )

        val totProvsnIncomeTax = Item(
            name = "tot_provsn_income_tax",
            description = "Tax Expense",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("tot_provsn_income_tax"),
                value = scale * fc.tot_provsn_income_tax.or0()
            ),
            type = ItemType.Custom,
            formula = "max(0,0.25 * ${preTaxIncome.name})"
        )

        val subsidiaireCostValue = (
                fc.minority_int.or0()
                        + fc.equity_earn_subsid.or0()
                        + fc.invst_gain_loss_other.or0()
                        + fc.other_income.or0()
                        + fc.income_discont_oper.or0()
                        + fc.exord_income_loss.or0()
                        + fc.cumul_eff_acct_change.or0()
                        + fc.non_ctl_int.or0()
                ) * scale

        val subsidiaireCost = Item(
            name = "subsidiaire_cost",
            description = "Subsidiaire Cost",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("subsidiaire_cost"),
                value = subsidiaireCostValue,
            ),
            type = ItemType.PercentOfRevenue,
            percentOfRevenue = PercentOfRevenue(percentOfRevenue = subsidiaireCostValue / revenue.historicalValue?.value.or0()),
        )

        val prefStockDivOtherAdj = Item(
            name = "pref_stock_div_other_adj",
            description = "Prefer Stock Dividend",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("pref_stock_div_other_adj"),
                value = fc.pref_stock_div_other_adj.or0() * scale,
            ),
            type = ItemType.FixedCost,
            fixedCost = FixedCost(cost = fc.pref_stock_div_other_adj.or0() * scale)
        )

        val netIncomeLossShareHolder = Item(
            name = "net_income_loss_share_holder",
            description = "Net Income",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("net_income_loss_share_holder"),
                value = fc.net_income_loss_share_holder.or0() * scale,
            ),
            type = ItemType.SumOfOtherItems,
            sumOfOtherItems = SumOfOtherItems(
                components = listOf(
                    Component(weight = 1.0, itemName = preTaxIncome.name),
                    Component(weight = -1.0, itemName = totProvsnIncomeTax.name),
                    Component(weight = -1.0, itemName = subsidiaireCost.name),
                    Component(weight = -1.0, itemName = prefStockDivOtherAdj.name),
                )
            )
        )

        val wavgSharesOutDiluted = Item(
            name = "wavg_shares_out_diluted",
            description = "Shares Outstanding",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("wavg_shares_out_diluted"),
                value = fc.wavg_shares_out_diluted.or0() * scale,
            ),
            type = ItemType.FixedCost,
            fixedCost = FixedCost(
                cost = scale * fc.wavg_shares_out_diluted.or0()
            ),
        )

        val epsDilutedNet = Item(
            name = "eps_diluted_net",
            description = "Earnings Per Share",
            historicalValue = HistoricalValue(
                documentPeriodEndDate = fc.per_end_date.toString(),
                factIds = listOf("eps_diluted_net"),
                value = fc.eps_diluted_net.or0(),
            ),
            type = ItemType.Custom,
            formula = "${netIncomeLossShareHolder.name} / ${wavgSharesOutDiluted.name}",
        )

        val model = Model(
            cik = mt.comp_cik,
            ticker = mt.ticker,
            name = fc.comp_name,
            totalRevenueConceptName = "tot_revnu",
            epsConceptName = "eps_diluted_net",
            netIncomeConceptName = "net_income_loss_share_holder",
            sharesOutstandingConceptName = "wavg_shares_out_diluted",
            beta = 1.0,
            riskFreeRate = 0.0,
            equityRiskPremium = 0.075,
            terminalGrowthRate = 0.025,
            periods = ses.size.coerceAtMost(10),
            excelColumnOffset = 1,
            excelRowOffset = 1,
            incomeStatementItems = listOf(
                revenue,
                costOfGoodsSold,
                grossProfit,
                resDevExp,
                totSellGenAdminExp,
                totDeprecAmort,
                otherOperatingExpense,
                operIncome,
                totNonOperIncomeExp,
                preTaxIncome,
                totProvsnIncomeTax,
                subsidiaireCost,
                prefStockDivOtherAdj,
                netIncomeLossShareHolder,
                wavgSharesOutDiluted,
                epsDilutedNet,
            ),
        )

        log.info(
            "Built Zacks model " +
                    "cik={}, " +
                    "ticker={}, " +
                    "name={}, " +
                    "beta={}, " +
                    "riskFreeRate={}, " +
                    "equityRiskPremium={}, " +
                    "terminalGrowthRate={}, " +
                    "periods={} ",
            model.cik,
            model.ticker,
            model.name,
            model.beta,
            model.riskFreeRate,
            model.equityRiskPremium,
            model.terminalGrowthRate,
            model.periods,
        )

        val stockAnalysis2 = stockAnalysisService.evaluateStockAnalysis(
            model = model,
            saveAs = "${model.cik}_zacks",
            published = true,
            tags = listOf("Automated", "Zacks")
        )

        return BuildZacksModelResponse(
            id = stockAnalysis2._id,
            cik = model.cik ?: return badInput("model.cik cannot be null"),
            ticker = model.ticker ?: return badInput("model.ticker cannot be null"),
            targetPrice = stockAnalysis2.derivedStockAnalytics?.targetPrice ?: 0.0,
            finalPrice = stockAnalysis2.derivedStockAnalytics?.finalPrice ?: 0.0,
        )
    }

    private fun mt(ticker: String) =
        postgresService.runSql(
            sql = """select * from mt where ticker = '$ticker'""".trimIndent(),
            clazz = MT::class
        ).firstOrNull()

    private fun ses(ticker: String) =
        postgresService.runSql(
            sql = """
                select *
                from se
                where ticker = '$ticker'
                  and per_type = 'A'
                  and per_end_date >= now()
                order by per_end_date desc 
            """.trimIndent(),
            clazz = SE::class
        ).toList()

    private fun fc(ticker: String) = postgresService.runSql(
        sql = """
            select *
            from fc
            where ticker = '$ticker'
              and per_type = 'A'
            order by per_end_date desc limit 1
        """.trimIndent(),
        clazz = FC::class
    ).firstOrNull()
}