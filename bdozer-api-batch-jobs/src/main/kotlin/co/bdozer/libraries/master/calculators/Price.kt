package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.polygon.Polygon

fun price(ticker: String): Double {
    val previousClose = Polygon.previousClose(ticker)
    return if (previousClose.resultsCount != 1) {
        error("Cannot find a price for $ticker")
    } else {
        previousClose.results.first().c
    }
}