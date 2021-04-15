package com.bdozer.edgar.factbase.ingestor.support

import com.google.common.hash.Hashing
import com.bdozer.edgar.dataclasses.XbrlContext
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import java.nio.charset.StandardCharsets
import java.time.LocalDate


class FactIdGenerator {

    fun generateId(
        conceptName: String,
        context: XbrlContext,
        documentPeriodEndDate: LocalDate,
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
                "$conceptName$entityId$documentFiscalPeriodFocus$documentPeriodEndDate$instant$startDate$endDate$explicitMembers",
                StandardCharsets.UTF_8
            )
            .toString()
    }

}
