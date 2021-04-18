package com.bdozer.stockanalyzer

import com.bdozer.stockanalyzer.dataclasses.EvaluateModelRequest
import com.bdozer.stockanalyzer.dataclasses.StockAnalysis2
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/stock-analyzer/workflow")
class StockAnalysisWorkflowController(
    private val statelessModelEvaluator: StatelessModelEvaluator,
) {

    @PostMapping("refresh")
    fun refresh(@RequestBody stockAnalysis: StockAnalysis2): StockAnalysis2 {
        TODO()
    }

    @Operation(
        description = """
        This API evaluates a model you've assembled and return a stock analysis object 
        
        The passed in Model represents high level relationship between the various financial statement items of underlying a stock 
        Calling this method evaluates those relationships and turn them into real numbers
        
        This API does not persist (save) the stock analysis. Please call the stock analysis service API to save the analysis
        
        This is a stateless calculator
    """
    )
    @PostMapping("evaluate")
    fun evaluate(@RequestBody request: EvaluateModelRequest): StockAnalysis2 {
        return statelessModelEvaluator.evaluate(request)
    }

}