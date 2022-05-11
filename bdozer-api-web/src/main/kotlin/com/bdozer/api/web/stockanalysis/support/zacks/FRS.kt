package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.web.stockanalysis.support.zacks.models.FR

data class FRS(
    val annuals: List<FR>,
    val quarters: List<FR>,
)