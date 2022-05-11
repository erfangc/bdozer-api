package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.stockanalysis.master.models.MT
import com.bdozer.api.stockanalysis.master.models.MarketData

data class RawData(
    val fcs: FCS,
    val frs: FRS,
    val mt: MT,
    val marketData: MarketData,
)
