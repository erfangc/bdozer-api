package com.bdozer.sec.dataclasses

import java.time.LocalDate

data class XbrlPeriod(
    val instant: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)
