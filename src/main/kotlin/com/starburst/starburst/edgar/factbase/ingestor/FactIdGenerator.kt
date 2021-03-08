package com.starburst.starburst.edgar.factbase.ingestor

import com.google.common.hash.Hashing
import com.starburst.starburst.edgar.dataclasses.XbrlContext
import org.w3c.dom.Element
import java.nio.charset.StandardCharsets


class FactIdGenerator {

    fun generateId(
        elementName: String,
        context: XbrlContext,
        documentPeriodEndDate: String
    ): String {

        val entityId = context.entity.identifier.value
        val period = context.period

        val instant = period.instant ?: "no_instant"
        val startDate = period.startDate ?: "no_startdate"
        val endDate = period.endDate ?: "no_enddate"

        val explicitMembers =
            context
                .entity
                .segment
                ?.explicitMembers
                ?.sortedBy { it.dimension + it.value }
                ?.joinToString()
                ?: "no_segment"

        return Hashing
            .sha256()
            .hashString(
                "$elementName$entityId$documentPeriodEndDate$instant$startDate$endDate$explicitMembers",
                StandardCharsets.UTF_8
            )
            .toString()
    }

}
