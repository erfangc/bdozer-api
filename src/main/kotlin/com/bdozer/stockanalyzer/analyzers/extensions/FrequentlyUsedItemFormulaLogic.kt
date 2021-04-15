package com.bdozer.stockanalyzer.analyzers.extensions

import com.bdozer.models.dataclasses.Commentary
import com.bdozer.models.dataclasses.Item
import com.bdozer.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.bdozer.xml.HttpClientExtensions.readXml
import java.io.File

object FrequentlyUsedItemFormulaLogic {

    fun AbstractStockAnalyzer.fillOneTimeItem(item: Item) = item.copy(
        formula = "0.0",
        commentaries = Commentary(commentary = "This is a one-time item")
    )

    fun AbstractStockAnalyzer.fillEpsItem(item: Item): Item {
        return item.copy(
            formula = "$netIncomeConceptName / $sharesOutstandingConceptName"
        )
    }

    fun AbstractStockAnalyzer.fillTaxItem(item: Item): Item {
        return if (ebitConceptName != null)
            item.copy(
                formula = "${ebitConceptName}*0.12"
            ) else
            item
    }

}
