package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.core.dataclasses.DocumentFiscalPeriodFocus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@CrossOrigin
@RequestMapping("api/fact-base/time-series")
class FactTimeSeriesController(
    private val timeSeriesService: TimeSeriesService
) {

    @GetMapping("{factId}")
    fun getTimeSeriesForFact(
        @RequestParam cik: String,
        @PathVariable factId: String,
        @RequestParam conceptNames: List<String>,
        @RequestParam(required = false) startDate: LocalDate? = null,
        @RequestParam(required = false) stopDate: LocalDate? = null,
        @RequestParam(required = false) documentFiscalPeriodFocus: DocumentFiscalPeriodFocus? = null,
        @RequestParam(required = false) prune: Boolean? = null,
    ): List<FactTimeSeries> {
        return timeSeriesService.getTimeSeriesForFact(
            cik = cik,
            factId = factId,
            conceptNames = conceptNames,
            startDate = startDate ?: LocalDate.now().minusYears(10),
            stopDate = stopDate ?: LocalDate.now(),
            documentFiscalPeriodFocus = documentFiscalPeriodFocus ?: DocumentFiscalPeriodFocus.FY,
            prune = prune ?: true,
        )
    }

    @GetMapping
    fun getTimeSeries(
        @RequestParam cik: String,
        @RequestParam conceptNames: List<String>,
        @RequestParam(required = false) startDate: LocalDate? = null,
        @RequestParam(required = false) stopDate: LocalDate? = null,
        @RequestParam(required = false) documentFiscalPeriodFocus: DocumentFiscalPeriodFocus? = null,
        @RequestParam(required = false) prune: Boolean? = null,
    ): List<FactTimeSeries> {
        return timeSeriesService.getTimeSeries(
            cik = cik,
            conceptNames = conceptNames,
            startDate = startDate ?: LocalDate.now().minusYears(10),
            stopDate = stopDate ?: LocalDate.now(),
            documentFiscalPeriodFocus = documentFiscalPeriodFocus ?: DocumentFiscalPeriodFocus.FY,
            prune = prune ?: true,
        )
    }
}