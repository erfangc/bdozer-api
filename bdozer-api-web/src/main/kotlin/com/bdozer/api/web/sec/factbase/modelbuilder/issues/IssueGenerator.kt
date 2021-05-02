package com.bdozer.api.web.sec.factbase.modelbuilder.issues

import com.bdozer.api.web.stockanalysis.dataclasses.StockAnalysis2
import org.springframework.stereotype.Service

/**
 * # Overview
 * [IssueGenerator] takes a [StockAnalysis2] and creates issues
 * after performing automated checks on the model
 *
 * The created issues can be persisted through [IssueManager],
 * issues identified by [IssueGenerator] and stored in [IssueManager] should be resolved
 * by a intervening user through a front-end UI or via automated API calls
 *
 * ## How are issue resolutions tracked?
 * The underlying issues may be dismissed (removed) via API calls to [IssueManager]
 * Obviously, [IssueManager] will not check to ensure that the issues are actually resolved
 * but simply mark them as such
 *
 * ## How to handle issues that are marked as resolved but are not actually resolved
 * To prevent against issues from being resolved prematurely, repeated invocations to [generateIssues]
 * on [IssueGenerator] can re-generate and resurface issues that are actually not resolved
 */
@Service
class IssueGenerator {

    fun generateIssues(stockAnalysis: StockAnalysis2): List<Issue> {
        val stockAnalysisId = stockAnalysis._id
        val model = stockAnalysis.model
        return IssueType.values().flatMap { issueType ->
            when (issueType) {
                IssueType.RevenueItemNotFound -> {
                    if (model.totalRevenueConceptName == null) {
                        listOf(
                            Issue(
                                _id = "$stockAnalysisId$issueType",
                                stockAnalysisId = stockAnalysisId,
                                issueType = issueType,
                                message = "Total revenue item is missing"
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
                IssueType.NetIncomeItemNotFound ->
                    if (model.netIncomeConceptName == null) {
                        listOf(
                            Issue(
                                _id = "$stockAnalysisId$issueType",
                                stockAnalysisId = stockAnalysisId,
                                issueType = issueType,
                                message = "Net income item is missing"
                            )
                        )
                    } else {
                        emptyList()
                    }
                IssueType.SharesOutstandingItemNotFound ->
                    if (model.sharesOutstandingConceptName == null) {
                        listOf(
                            Issue(
                                _id = "$stockAnalysisId$issueType",
                                stockAnalysisId = stockAnalysisId,
                                issueType = issueType,
                                message = "Shares outstanding item is missing"
                            )
                        )
                    } else {
                        emptyList()
                    }
                IssueType.EpsItemNotFound ->
                    if (model.epsConceptName == null) {
                        listOf(
                            Issue(
                                _id = "$stockAnalysisId$issueType",
                                stockAnalysisId = stockAnalysisId,
                                issueType = issueType,
                                message = "EPS item is missing"
                            )
                        )
                    } else {
                        emptyList()
                    }

            }
        }
    }

}
