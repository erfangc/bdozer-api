package com.starburst.starburst.dcf

import com.starburst.starburst.Provier.fictitiousSaaSCompany
import com.starburst.starburst.models.builders.ModelBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DCFCalculatorTest {

    @Test
    fun performDcf() {
        val model = fictitiousSaaSCompany()
        val output = ModelBuilder().evaluateModel(model)
        assertEquals(99.74034061511966, output.targetPriceUnderExitMultipleMethod)
        assertEquals(154.21122566489873, output.targetPriceUnderPerpetuityMethod)
    }

}
