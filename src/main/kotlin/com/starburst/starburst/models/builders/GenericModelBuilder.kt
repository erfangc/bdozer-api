package com.starburst.starburst.models.builders

import com.starburst.starburst.models.Item
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ReservedNames.CostOfGoodsSold
import com.starburst.starburst.models.ReservedNames.GrossProfit
import com.starburst.starburst.models.ReservedNames.InterestExpense
import com.starburst.starburst.models.ReservedNames.NetIncome
import com.starburst.starburst.models.ReservedNames.NonOperatingExpense
import com.starburst.starburst.models.ReservedNames.OperatingExpense
import com.starburst.starburst.models.ReservedNames.OperatingIncome
import com.starburst.starburst.models.ReservedNames.Revenue
import com.starburst.starburst.models.ReservedNames.TaxExpense

class GenericModelBuilder {

    /**
     * [createModel] creates the skeleton of a basic model
     * this doesn't have to be the only skeleton model available
     */
    fun createModel(): Model {
        return Model(
            items = listOf(
                Item(
                    name = Revenue,
                    expression = "0.0"
                ),
                Item(
                    name = CostOfGoodsSold,
                    expression = "0.0"
                ),
                Item(
                    name = GrossProfit,
                    expression = "$Revenue - $CostOfGoodsSold"
                ),
                Item(
                    name = OperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = OperatingIncome,
                    expression = "$GrossProfit - $OperatingExpense"
                ),
                Item(
                    name = NonOperatingExpense,
                    expression = "0.0"
                ),
                Item(
                    name = InterestExpense,
                    expression = "0.0"
                ),
                Item(
                    name = TaxExpense,
                    expression = "0.0"
                ),
                Item(
                    name = NetIncome,
                    expression = "$OperatingIncome - $NonOperatingExpense - $InterestExpense - $TaxExpense"
                )
            ),
            periods = 5
        )
    }

}
