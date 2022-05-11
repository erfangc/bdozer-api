package com.bdozer.api.stockanalysis.models

data class PerShareMetrics(

    val epsBasicNet: Double? = null,
    val epsBasicContOper: Double? = null,
    val epsBasicDiscontOper: Double? = null,
    val epsBasicExtra: Double? = null,

    val priceToEpsBasicNet: Double? = null,
    val priceToEpsBasicContOper: Double? = null,
    val priceToEpsBasicDiscontOper: Double? = null,
    val priceToEpsBasicExtra: Double? = null,

    val epsDilutedNet: Double? = null,
    val epsDilutedContOper: Double? = null,
    val epsDilutedDiscontOper: Double? = null,
    val epsDilutedExtra: Double? = null,

    val priceToEpsDilutedNet: Double? = null,
    val priceToEpsDilutedContOper: Double? = null,
    val priceToEpsDilutedDiscontOper: Double? = null,
    val priceToEpsDilutedExtra: Double? = null,

    val freeCashFlowPerShare: Double? = null,
    val priceToFreeCashFlowPerShare: Double? = null,

    val operCashFlowPerShare: Double? = null,
    val priceToOperCashFlowPerShare: Double? = null,

    val bookValPerShare: Double? = null,
    val priceToBookValPerShare: Double? = null,

    )