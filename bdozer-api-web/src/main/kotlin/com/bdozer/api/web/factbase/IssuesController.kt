package com.bdozer.api.web.factbase

import com.bdozer.api.factbase.modelbuilder.issues.Issue
import com.bdozer.api.factbase.modelbuilder.issues.IssueGenerator
import com.bdozer.api.factbase.modelbuilder.issues.IssueManager
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RequestMapping("api/issues")
@CrossOrigin
@RestController
class IssuesController(
    private val issueManager: IssueManager,
) {
    @PostMapping("generate-issues")
    fun generateIssues(@RequestBody stockAnalysis: StockAnalysis2): List<Issue> {
        return IssueGenerator().generateIssues(stockAnalysis)
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