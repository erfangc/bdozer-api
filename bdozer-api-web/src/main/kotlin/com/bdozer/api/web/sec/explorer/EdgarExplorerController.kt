package com.bdozer.api.web.sec.explorer

import com.bdozer.api.web.sec.explorer.dataclasses.EdgarEntity
import com.bdozer.api.web.sec.explorer.dataclasses.EdgarFilingMetadata
import org.springframework.web.bind.annotation.*

/**
 * This REST Controller basically is a pass through to the SEC's own Elasticsearch
 * APIs to find entities by symbol, name CIK as well as their filings
 *
 *  We do a pass through here to cure any CORS issues
 */
@RestController
@CrossOrigin
@RequestMapping("public/edgar-explorer")
class EdgarExplorerController(
    private val edgarExplorer: EdgarExplorer
) {

    @GetMapping("entities", produces = ["application/json"])
    fun searchEntities(@RequestParam term: String): List<EdgarEntity?> {
        return edgarExplorer.searchEntities(term)
    }

    @GetMapping("filings", produces = ["application/json"])
    fun searchFilings(@RequestParam cik: String): List<EdgarFilingMetadata> {
        return edgarExplorer.searchFilings(cik)
    }

}