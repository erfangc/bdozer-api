package co.bdozer.libraries.master

import co.bdozer.libraries.master.calculators.FCS
import co.bdozer.libraries.master.calculators.FRS
import co.bdozer.libraries.master.models.MarketData
import co.bdozer.libraries.zacks.models.MT

data class RawData(
    val fcs: FCS,
    val frs: FRS,
    val mt: MT,
    val marketData: MarketData,
)
