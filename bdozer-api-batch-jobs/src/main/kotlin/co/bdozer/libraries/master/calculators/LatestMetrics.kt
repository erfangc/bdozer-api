package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.master.RawData
import co.bdozer.libraries.master.models.LatestMetrics

fun latestMetrics(rawData: RawData): LatestMetrics {
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

    return LatestMetrics(
        revenue = revenue,
        ebitda = ebitda,
        ebit = ebit,
        netIncome = netIncome,
        debtToEquity = debt / equity,
        debtToAsset = debt / asset,
        totalAsset = asset,
        totalLiability = debt,
        longTermDebt = ltDebt,
        longTermDebtToAsset = ltDebt / asset,
    )
}