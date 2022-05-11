package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.stockanalysis.master.models.MarketData
import com.bdozer.api.stockanalysis.master.models.FC
import com.bdozer.api.web.stockanalysis.support.zacks.models.FR
import com.bdozer.api.stockanalysis.master.models.MT
import com.bdozer.api.stockanalysis.models.ZacksDerivedAnalytics
import com.bdozer.api.stockanalysis.models.LatestMetrics
import com.bdozer.api.stockanalysis.models.PerShareMetrics
import com.bdozer.api.stockanalysis.models.Trend
import com.bdozer.api.web.stockanalysis.PostgresService
import com.bdozer.api.web.stockanalysis.support.poylgon.PolygonService
import com.bdozer.api.web.stockanalysis.support.poylgon.TickerDetail
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ZacksDerivedAnalyticsCalculator(
    private val postgresService: PostgresService,
    private val polygonService: PolygonService,
) {
    
    private val log = LoggerFactory.getLogger(ZacksDerivedAnalyticsCalculator::class.java)
    
    fun getZacksDerivedAnalytics(ticker: String): ZacksDerivedAnalytics {
        
        val tickerDetailV3 = polygonService.tickerDetails(ticker)
        val marketCap = tickerDetailV3.marketcap

        val price = price(ticker)
        val mt = mt(ticker)
        val frs = frs(ticker)
        val fcs = fcs(ticker)

        val enterpriseValue = enterpriseValue(tickerDetailV3, fcs)
        val rawData = RawData(
            mt = mt,
            fcs = fcs,
            frs = frs,
            marketData = MarketData(
                enterpriseValue = enterpriseValue,
                price = price,
                marketCap = marketCap,
            ),
        )

        return ZacksDerivedAnalytics(
            id = ticker,
            ticker = ticker,
            enterpriseValue = enterpriseValue,
            marketCap = marketCap,
            price = price,
            cik = mt.comp_cik,
            exchange = mt.exchange,
            companyName = mt.comp_name ?: mt.comp_name_2,
            companyUrl = mt.comp_url,
            earnings = trend(fcs.quarters) { it.net_income_loss },
            sales = trend(fcs.quarters) { it.tot_revnu },
            latestMetrics = latestMetrics(rawData),
            perShareMetrics = perShareMetrics(rawData),
            tags = emptyList(),
        ).let { it.copy(tags = TagGenerator().generateTags(it)) }
        
    }

    private fun fcs(ticker: String): FCS {
        @Language("PostgreSQL") val results = postgresService.runSql(
            sql = """
            select *
            from fc
            where ticker = '$ticker' 
            order by qtr_nbr desc 
            """.trimIndent(), FC::class
        ).toList()

        val groupBy = results.groupBy { it.per_type }
        val annuals = groupBy["A"]?.take(2) ?: emptyList()
        val quarters = groupBy["Q"]?.take(6) ?: emptyList()

        log.info("Loaded fundamentals characteristics for $ticker")
        return FCS(annuals = annuals, quarters = quarters)
    }
    fun price(ticker: String): Double {
        return polygonService.previousClose(ticker).results.first().c!!
    }

    private fun latestMetrics(rawData: RawData): LatestMetrics {

        val (fcs, _, _) = rawData

        val revenue = fcs.quarters.sumOf { it.tot_revnu ?: 0.0 }
        val ebit = fcs.quarters.sumOf { it.ebit ?: 0.0 }
        val ebitda = fcs.quarters.sumOf { it.ebitda ?: 0.0 }
        val netIncome = fcs.quarters.sumOf { it.net_income_loss ?: 0.0 }

        val latestQ = fcs.quarters.first()
        val latestA = fcs.annuals.first()

        val latest = if (latestQ.per_end_date?.isAfter(latestA.per_end_date) == true) {
            latestQ
        } else {
            latestA
        }

        val equity = latest.tot_share_holder_equity ?: 0.0
        val debt = latest.tot_liab ?: 0.0
        val ltDebt = latest.tot_lterm_debt ?: 0.0
        val asset = latest.tot_asset ?: 0.0

        fun Double.clean(): Double? {
            return if (this.isNaN() || this.isInfinite()) {
                null
            } else {
                this
            }
        }

        return LatestMetrics(
            revenue = revenue.clean(),
            ebitda = ebitda.clean(),
            ebit = ebit.clean(),
            netIncome = netIncome.clean(),
            debtToEquity = (debt / equity).clean(),
            debtToAsset = (debt / asset).clean(),
            totalAsset = asset.clean(),
            totalLiability = debt.clean(),
            longTermDebt = ltDebt.clean(),
            longTermDebtToAsset = (ltDebt / asset).clean(),
        )

    }
    private fun perShareMetrics(rawData: RawData): PerShareMetrics {

        val fr = rawData.frs.quarters.take(4)
        val fc = rawData.fcs.quarters.take(4)
        val price = rawData.marketData.price ?: 0.0

        fun sum(extractor: (FC) -> Double?): Double {
            return fc.sumOf { extractor.invoke(it) ?: 0.0 }
        }

        fun sumR(extractor: (FR) -> Double?): Double {
            return fr.sumOf { extractor.invoke(it) ?: 0.0 }
        }

        fun priceTo(denom: Double?): Double? {
            return if (denom == null || denom <= 0.0 || denom.isNaN() || denom.isInfinite()) {
                null
            } else {
                price / denom
            }
        }

        return PerShareMetrics(
            epsBasicNet = sum { it.eps_basic_net },
            epsBasicContOper = sum { it.eps_basic_cont_oper },
            epsBasicDiscontOper = sum { it.eps_basic_discont_oper },
            epsBasicExtra = sum { it.eps_basic_extra },

            priceToEpsBasicNet = priceTo(sum { it.eps_basic_net }),
            priceToEpsBasicContOper = priceTo(sum { it.eps_basic_cont_oper }),
            priceToEpsBasicDiscontOper = priceTo(sum { it.eps_basic_discont_oper }),
            priceToEpsBasicExtra = priceTo(sum { it.eps_basic_extra }),

            epsDilutedNet = sum { it.eps_diluted_net },
            epsDilutedContOper = sum { it.eps_diluted_cont_oper },
            epsDilutedDiscontOper = sum { it.eps_diluted_discont_oper },
            epsDilutedExtra = sum { it.eps_diluted_extra },

            priceToEpsDilutedNet = priceTo(sum { it.eps_diluted_net }),
            priceToEpsDilutedContOper = priceTo(sum { it.eps_diluted_cont_oper }),
            priceToEpsDilutedDiscontOper = priceTo(sum { it.eps_diluted_discont_oper }),
            priceToEpsDilutedExtra = priceTo(sum { it.eps_diluted_extra }),

            freeCashFlowPerShare = sumR { it.free_cash_flow_per_share },
            priceToFreeCashFlowPerShare = priceTo(sumR { it.free_cash_flow_per_share }),

            operCashFlowPerShare = sumR { it.oper_cash_flow_per_share },
            priceToOperCashFlowPerShare = priceTo(sumR { it.oper_cash_flow_per_share }),

            bookValPerShare = sumR { it.book_val_per_share },
            priceToBookValPerShare = priceTo(sumR { it.book_val_per_share }),
        )
    }
    
    private fun enterpriseValue(tickerDetailV3: TickerDetail, fcs: FCS): Double {
        val totLiab = fcs.quarters.first().tot_liab
        val marketCap = tickerDetailV3.marketcap ?: 0.0
        return marketCap + (totLiab ?: 0.0)
    }
    private fun mt(ticker: String): MT {
        @Language("PostgreSQL") val result = postgresService.runSql(
            sql = """
        select * 
        from mt 
        where ticker = '$ticker'
    """.trimIndent(), MT::class
        ).first()

        log.info("Loaded master table for $ticker")
        return result
    }

    private fun frs(ticker: String): FRS {
        @Language("PostgreSQL") val results = postgresService
            .runSql(
                sql = """
                select *
                from fr
                where ticker = '$ticker'
                order by per_end_date desc
                """.trimIndent(),
                FR::class,
            ).toList()
        val groupBy = results.groupBy { it.per_type }
        val annuals = groupBy["A"]?.take(2) ?: emptyList()
        val quarters = groupBy["Q"]?.take(6) ?: emptyList()

        log.info("Loaded fundamental ratios for $ticker")
        return FRS(annuals = annuals, quarters = quarters)
    }
    private fun trend(fcs: List<FC>, extractor: (FC) -> Double?): Trend {
        val fcs = fcs.take(6).sortedByDescending { it.qtr_nbr }
        val pctChanges = fcs
            .windowed(2)
            .map {
                val curr = extractor.invoke(it.first()) ?: 0.0
                val prev = extractor.invoke(it.last())
                if (prev == null || prev == 0.0 || prev.isNaN()) {
                    0.0
                } else {
                    // if prev value and curr value have different signs the % change won't be meaningful
                    // in that case we return nothing
                    if (curr * prev < 0) {
                        0.0
                    } else if (curr < 0 && prev < 0) {
                        // if prev and current value are both negative, the percentage change will
                        // also not be meaningful
                        0.0
                    } else {
                        (curr - prev) / prev
                    }
                }
            }

        val changes = fcs
            .windowed(2)
            .map {
                val curr = extractor.invoke(it.first()) ?: 0.0
                val prev = extractor.invoke(it.last()) ?: 0.0
                curr - prev
            }

        // we say growth rate increasing if the most recent period is increasing
        // and there are more increases than decreases in the remaining period
        val isIncreasing = if (changes.isNotEmpty()) {
            if (changes[0] > 0) {
                // the latest change is positive
                val takes = changes.take(4)
                val numPositiveChanges = takes
                    .filter { it > 0 }
                    .size
                numPositiveChanges > (takes.size / 2)
            } else {
                false
            }
        } else {
            false
        }

        // we say growth rate is erratic if there are roughly equal number of increases and decreases
        val take = changes.take(5)
        val isErratic = (take.filter { it < 0 }.size - take.filter { it > 0 }.size) < 2

        return Trend(
            isErratic = isErratic,
            isIncreasing = isIncreasing,

            thisQuarter = if (changes.isNotEmpty()) changes[0] else null,
            oneQuarterAgo = if (changes.size > 1) changes[1] else null,
            twoQuartersAgo = if (changes.size > 2) changes[2] else null,
            threeQuartersAgo = if (changes.size > 3) changes[3] else null,
            fourQuartersAgo = if (changes.size > 4) changes[4] else null,

            thisQuarterPctChange = if (pctChanges.isNotEmpty()) pctChanges[0] else null,
            oneQuarterAgoPctChange = if (pctChanges.size > 1) pctChanges[1] else null,
            twoQuartersAgoPctChange = if (pctChanges.size > 2) pctChanges[2] else null,
            threeQuartersAgoPctChange = if (pctChanges.size > 3) pctChanges[3] else null,
            fourQuartersAgoPctChange = if (pctChanges.size > 4) pctChanges[4] else null,
        )
    }
}
