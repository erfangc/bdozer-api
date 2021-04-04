package com.starburst.starburst.modelbuilder.common

import com.starburst.starburst.models.dataclasses.Model
import com.starburst.starburst.zacks.fa.ZacksFundamentalA
import com.starburst.starburst.zacks.se.ZacksSalesEstimates

data class Context(val model: Model, val zacksSalesEstimates: List<ZacksSalesEstimates>)