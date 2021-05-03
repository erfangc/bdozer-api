package bdozer.api.common.model

data class Item(
    /**
     * [name] of this item, this is akin to an identifier
     */
    val name: String,

    /**
     * [description] for human reading
     */
    val description: String? = null,

    val type: ItemType = ItemType.Custom,

    /**
     * [historicalValue] the latest actual value for this item
     */
    val historicalValue: HistoricalValue? = null,

    val formula: String = "0.0",

    val sumOfOtherItems: SumOfOtherItems? = null,
    val manualProjections: ManualProjections? = null,
    val percentOfRevenue: PercentOfRevenue? = null,
    val percentOfAnotherItem: PercentOfAnotherItem? = null,
    val compoundedGrowth: CompoundedGrowth? = null,
    val fixedCost: FixedCost? = null,

    val commentaries: Commentary? = null,
)

