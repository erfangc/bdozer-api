package com.bdozer.api.web.semanticsearch

import com.bdozer.api.web.semanticsearch.models.SemanticSearchResponse
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/semantic-search")
class SemanticSearchController(
    private val semanticSearchService: SemanticSearchService
) {
    @PostMapping
    fun semanticSearch(
        @RequestParam(required = false) ticker: String? = null, 
        @RequestParam question: String
    ): SemanticSearchResponse {
        return semanticSearchService.semanticSearch(ticker, question)
    }
}