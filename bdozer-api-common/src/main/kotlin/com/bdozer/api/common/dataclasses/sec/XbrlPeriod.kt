package com.bdozer.api.common.dataclasses.sec

import java.time.LocalDate

data class XbrlPeriod(
    val instant: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)
