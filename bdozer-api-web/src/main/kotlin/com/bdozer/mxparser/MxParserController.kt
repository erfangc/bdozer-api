package com.bdozer.mxparser

import org.mariuszgromada.math.mxparser.Expression
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/mxparser")
@CrossOrigin
class MxParserController {
    @PostMapping
    fun evaluate(@RequestBody body: MxParserEvaluateRequest): MxParserEvaluateResponse {
        val expression = Expression(body.formula)
        val error = if (expression.checkSyntax()) null else expression.errorMessage
        val value = expression.calculate()
        return MxParserEvaluateResponse(
            value = if (value.isNaN()) null else value,
            error = error
        )
    }
}
