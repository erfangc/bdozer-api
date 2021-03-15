package com.starburst.starburst.zacks.modelbuilder

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.starburst.starburst.models.Utility.DiscountFactor
import com.starburst.starburst.models.Utility.EarningsPerShare
import com.starburst.starburst.models.Utility.NetIncome
import com.starburst.starburst.models.Utility.PresentValuePerShare
import com.starburst.starburst.models.Utility.SharesOutstanding
import com.starburst.starburst.models.Utility.TerminalValuePerShare
import com.starburst.starburst.models.dataclasses.Item
import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.dataclasses.ZacksFundamentalA
import com.vhl.blackmo.grass.dsl.grass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@ExperimentalStdlibApi
@Service
class ZacksModelBuilder(keyInputsProvider: KeyInputsProvider) {

    private val incomeStatementItemsBuilder = IncomeStatementItemsBuilder(keyInputsProvider)
    private val balanceSheetItemsBuilder = BalanceSheetItemsBuilder()
    private val zacksFundamentalAs: List<ZacksFundamentalA>
    private val fileName = "/Users/erfangchen/Downloads/ZACKS_FC_addc6c96afcc63aaedeb3dae8c933d5a.csv"
    private val log = LoggerFactory.getLogger(ZacksModelBuilder::class.java)

    init {
        val csvContents = csvReader().readAllWithHeader(File(fileName))
        zacksFundamentalAs = grass<ZacksFundamentalA> { dateFormat = "M/d/yy" }.harvest(csvContents)
        log.info("Loaded ${zacksFundamentalAs.size} ${zacksFundamentalAs.javaClass.simpleName} from $fileName")
    }

    /**
     * Build a model using Zacks Fundamental A data
     * for the given ticker
     */
    fun buildModel(ticker: String): Model {

        val fundamentalA = findZacksFundamentalA(ticker)
        val incomeStatementItems = incomeStatementItemsBuilder.incomeStatementItems(fundamentalA)
        val balanceSheetItems = balanceSheetItemsBuilder.balanceSheetItems(fundamentalA)

        val model = Model(
            symbol = ticker,
            name = fundamentalA.comp_name ?: "N/A",
            incomeStatementItems = incomeStatementItems,
            balanceSheetItems = balanceSheetItems,
            cashFlowStatementItems = listOf(),
            otherItems = listOf(),
        )

        return model.copy(otherItems = deriveOtherItems(model))

    }

    private fun deriveOtherItems(model: Model): List<Item> {
        val periods = model.periods
        val discountRate = (model.equityRiskPremium * model.beta) + model.riskFreeRate
        return listOf(
            Item(
                name = DiscountFactor,
                expression = "1 / (1.0 + $discountRate)^period"
            ),
            Item(
                name = EarningsPerShare,
                expression = "$NetIncome / $SharesOutstanding"
            ),
            Item(
                name = TerminalValuePerShare,
                expression = "if(period=$periods,$EarningsPerShare * ${model.terminalFcfMultiple},0.0)"
            ),
            Item(
                name = PresentValuePerShare,
                // TODO figure out the correct numerator if we can actually derive FCF
                expression = "$DiscountFactor * ($EarningsPerShare + $TerminalValuePerShare)"
            )
        )
    }

    private fun findZacksFundamentalA(ticker: String) = (zacksFundamentalAs
        .find { it.ticker == ticker }
        ?: error("Zacks fundamentals for $ticker not found"))

}
