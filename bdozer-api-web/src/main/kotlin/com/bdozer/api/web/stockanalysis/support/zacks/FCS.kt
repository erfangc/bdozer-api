package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.stockanalysis.master.models.FC

data class FCS(
    val annuals: List<FC>,
    val quarters: List<FC>,
)