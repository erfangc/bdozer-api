package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.web.stockanalysis.support.zacks.models.FC

data class FCS(
    val annuals: List<FC>,
    val quarters: List<FC>,
)