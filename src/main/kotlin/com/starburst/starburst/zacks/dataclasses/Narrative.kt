package com.starburst.starburst.zacks.dataclasses

import com.starburst.starburst.models.dataclasses.Model

data class Narrative(
    val model: Model,
    val revenueTalkingPoint: TalkingPoint,
    val variableCostTalkingPoint: TalkingPoint,
    val fixedCostTalkingPoint: TalkingPoint,
    val otherExpensesTalkingPoint: TalkingPoint,
    val epsTalkingPoint: TalkingPoint,
    val noGrowthValueTalkingPoint: TalkingPoint,
    val growthTalkingPoint: TalkingPoint,
    val targetPriceTalkingPoint: TalkingPoint,
)