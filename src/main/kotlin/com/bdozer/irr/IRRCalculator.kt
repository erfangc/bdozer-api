package com.bdozer.irr

import org.slf4j.LoggerFactory

/**
 * Calculates the internal rate of return.
 *
 * Syntax is IRR(values) or IRR(values,guess)
 *
 * @see [Wikipedia on IRR](http://en.wikipedia.org/wiki/Internal_rate_of_return.Numerical_solution)
 *
 * @see [Excel IRR](http://office.microsoft.com/en-us/excel-help/irr-HP005209146.aspx)
 */
object IRRCalculator {

    private val log = LoggerFactory.getLogger(IRRCalculator::class.java)
    /**
     * Computes the internal rate of return using an estimated irr of 10 percent.
     *
     * @param income the income values.
     * @return the irr.
     */
    fun irr(income: DoubleArray): Double? {
        return irr(income, 0.1)
    }

    /**
     * Calculates IRR using the Newton-Raphson Method.
     *
     *
     * Starting with the guess, the method cycles through the calculation until the result
     * is accurate within 0.00001 percent. If IRR can't find a result that works
     * after 20 tries, the Double.NaN<> is returned.
     *
     *
     *
     * The implementation is inspired by the NewtonSolver from the Apache Commons-Math library,
     * @see [http://commons.apache.org](http://commons.apache.org)
     *
     *
     *
     * @param values        the income values.
     * @param guess         the initial guess of irr.
     * @return the irr value. The method returns `Double.NaN`
     * if the maximum iteration count is exceeded
     *
     * @see [
     * http://en.wikipedia.org/wiki/Internal_rate_of_return.Numerical_solution](http://en.wikipedia.org/wiki/Internal_rate_of_return.Numerical_solution)
     *
     * @see [
     * http://en.wikipedia.org/wiki/Newton%27s_method](http://en.wikipedia.org/wiki/Newton%27s_method)
     */
    fun irr(values: DoubleArray, guess: Double): Double? {
        try {
            val maxIterationCount = 20
            val absoluteAccuracy = 1E-7
            var x0 = guess
            var x1: Double
            var i = 0
            while (i < maxIterationCount) {

                // the value of the function (NPV) and its derivate can be calculated in the same loop
                val factor = 1.0 + x0
                var k = 0
                var fValue = values[k]
                var fDerivative = 0.0
                var denominator = factor
                while (++k < values.size) {
                    val value = values[k]
                    fValue += value / denominator
                    denominator *= factor
                    fDerivative -= k * value / denominator
                }

                // the essense of the Newton-Raphson Method
                x1 = x0 - fValue / fDerivative
                if (Math.abs(x1 - x0) <= absoluteAccuracy) {
                    return x1
                }
                x0 = x1
                ++i
            }
            // maximum number of iterations is exceeded
            return Double.NaN
        } catch (e: Exception) {
            log.error("cannot compute IRR", e)
            return null
        }
    }
}