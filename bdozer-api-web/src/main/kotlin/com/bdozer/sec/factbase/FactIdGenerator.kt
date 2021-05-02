package com.bdozer.sec.factbase

import com.bdozer.api.common.dataclasses.sec.XbrlContext
import com.bdozer.api.common.dataclasses.sec.DocumentFiscalPeriodFocus
import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets


class FactIdGenerator {

    fun generateId(
        conceptName: String,
        context: XbrlContext,
        documentFiscalPeriodFocus: DocumentFiscalPeriodFocus,
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
                "$conceptName$entityId$documentFiscalPeriodFocus$instant$startDate$endDate$explicitMembers",
                StandardCharsets.UTF_8
            )
            .toString()
    }

}
