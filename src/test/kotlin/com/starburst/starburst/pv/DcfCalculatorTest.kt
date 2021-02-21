package com.starburst.starburst.pv

import com.starburst.starburst.Provier.fictitiousSaaSCompany
import com.starburst.starburst.models.builders.ModelBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DcfCalculatorTest {

    @Test
    fun calcPv() {
        val model = fictitiousSaaSCompany()
        val output = ModelBuilder().evaluateModel(model)
        assertEquals(89.7663065536077, output.targetPriceUnderExitMultipleMethod)
        assertEquals(138.79010309840885, output.targetPriceUnderPerpetuityMethod)
    }

}
