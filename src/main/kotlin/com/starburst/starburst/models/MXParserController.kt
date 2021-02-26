package com.starburst.starburst.models

import org.mariuszgromada.math.mxparser.Expression
import org.springframework.web.bind.annotation.*

data class MXParserEvaluateRequest(
    val formula: String
)

data class MXParserEvaluateResponse(
    val value: Double? = null,
    val error: String? = null
)

@RestController
@RequestMapping("api/mxparser")
@CrossOrigin
class MXParserController {
    @PostMapping
    fun evaluate(@RequestBody body: MXParserEvaluateRequest): MXParserEvaluateResponse {
        val expression = Expression(body.formula)
        val error = if (expression.checkSyntax()) null else expression.errorMessage
        val value = expression.calculate()
        return MXParserEvaluateResponse(
            value = if (value.isNaN()) null else value,
            error = error
        )
    }
}
