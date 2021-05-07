package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.api.factbase.core.dataclasses.Fact
import java.time.LocalDate

data class FactTimeSeries(
    val facts:List<Fact>,
    val conceptName: String,
    val label: String?,
    val startDate: LocalDate,
    val stopDate: LocalDate,
    val documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
)