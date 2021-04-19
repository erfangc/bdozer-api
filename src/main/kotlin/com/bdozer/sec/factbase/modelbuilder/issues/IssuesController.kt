package com.bdozer.sec.factbase.modelbuilder.issues

import com.bdozer.stockanalysis.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RequestMapping("api/issues")
@CrossOrigin
@RestController
class IssuesController(
    private val issueGenerator: IssueGenerator,
    private val issueManager: IssueManager,
) {
    @PostMapping("generate-issues")
    fun generateIssues(@RequestBody stockAnalysis: StockAnalysis2): List<Issue> {
        return issueGenerator.generateIssues(stockAnalysis)
    }

    @GetMapping
    fun findIssues(@RequestParam stockAnalysisId: String): List<Issue> {
        return issueManager.findIssues(stockAnalysisId)
    }

    @PostMapping
    fun saveIssues(@RequestBody documents: List<Issue>) {
        return issueManager.saveIssues(documents)
    }

    @DeleteMapping("{id}")
    fun deleteIssue(@PathVariable id: String) {
        return issueManager.deleteIssue(id)
    }
}