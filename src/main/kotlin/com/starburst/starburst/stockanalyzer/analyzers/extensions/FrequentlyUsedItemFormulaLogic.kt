package com.starburst.starburst.stockanalyzer.analyzers.extensions

import com.starburst.starburst.models.dataclasses.Commentary
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.stockanalyzer.analyzers.AbstractStockAnalyzer
import com.starburst.starburst.xml.HttpClientExtensions.readXml
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
