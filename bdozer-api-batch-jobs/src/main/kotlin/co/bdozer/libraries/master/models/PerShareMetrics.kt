package co.bdozer.libraries.master.models

data class PerShareMetrics(

    val epsBasicNet: Double?,
    val epsBasicContOper: Double?,
    val epsBasicDiscontOper: Double?,
    val epsBasicExtra: Double?,

    val priceToEpsBasicNet: Double?,
    val priceToEpsBasicContOper: Double?,
    val priceToEpsBasicDiscontOper: Double?,
    val priceToEpsBasicExtra: Double?,

    val epsDilutedNet: Double?,
    val epsDilutedContOper: Double?,
    val epsDilutedDiscontOper: Double?,
    val epsDilutedExtra: Double?,

    val priceToEpsDilutedNet: Double?,
    val priceToEpsDilutedContOper: Double?,
    val priceToEpsDilutedDiscontOper: Double?,
    val priceToEpsDilutedExtra: Double?,

    val freeCashFlowPerShare: Double?,
    val priceToFreeCashFlowPerShare: Double?,

    val operCashFlowPerShare: Double?,
    val priceToOperCashFlowPerShare: Double?,

    val bookValPerShare: Double?,
    val priceToBookValPerShare: Double?,

    )