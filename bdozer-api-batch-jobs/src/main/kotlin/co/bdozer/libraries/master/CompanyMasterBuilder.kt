package co.bdozer.libraries.master

import co.bdozer.libraries.master.calculators.*
import co.bdozer.libraries.master.models.CompanyMasterRecord
import co.bdozer.libraries.master.models.MarketData
import co.bdozer.libraries.polygon.Polygon.tickerDetailV3
import co.bdozer.libraries.polygon.models.TickerDetailV3

object CompanyMasterBuilder {

    fun buildCompanyRecord(ticker: String): CompanyMasterRecord {
        val tickerDetailV3 = tickerDetailV3(ticker)
        val marketCap = tickerDetailV3.results.market_cap

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

        return CompanyMasterRecord(
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
        )
    }

    private fun enterpriseValue(tickerDetailV3: TickerDetailV3, fcs: FCS): Double {
        val totLiab = fcs.quarters.first().tot_liab
        return tickerDetailV3.results.market_cap + (totLiab ?: 0.0)
    }

}
