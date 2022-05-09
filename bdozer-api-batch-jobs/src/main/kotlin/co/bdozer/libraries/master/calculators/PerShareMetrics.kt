package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.master.RawData
import co.bdozer.libraries.master.models.PerShareMetrics
import co.bdozer.libraries.zacks.models.FC
import co.bdozer.libraries.zacks.models.FR

fun perShareMetrics(rawData: RawData): PerShareMetrics {

    val fr = rawData.frs.quarters.take(4)
    val fc = rawData.fcs.quarters.take(4)
    val price = rawData.marketData.price

    fun sum(extractor: (FC) -> Double?): Double {
        return fc.sumOf { extractor.invoke(it) ?: 0.0 }
    }

    fun sumR(extractor: (FR) -> Double?): Double {
        return fr.sumOf { extractor.invoke(it) ?: 0.0 }
    }

    fun priceTo(denom: Double?): Double? {
        return if (denom == null || denom == 0.0) {
            null
        } else if (denom < 0) {
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
