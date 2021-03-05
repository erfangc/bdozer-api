package com.starburst.starburst.edgar.explorer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BasicHttpEntity
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
    fun searchEntities(@RequestParam term: String): JsonNode {
        return edgarExplorer.searchEntities(term)
    }

    @GetMapping("filings", produces = ["application/json"])
    fun searchFilings(@RequestParam cik: String): List<EdgarFilingMetadata> {
        return edgarExplorer.searchFilings(cik)
    }

}