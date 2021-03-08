package com.starburst.starburst.edgar.factbase.ingestor

import com.google.common.hash.Hashing
import com.starburst.starburst.edgar.dataclasses.XbrlContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.charset.StandardCharsets


class FactIdGenerator(instanceDocument: Element) {

    fun generateId(node: Node, context: XbrlContext, documentPeriodEndDate: String): String {

        val nodeName = node.nodeName
        val entityId = context.entity.identifier.value
        val instant = context.period.instant ?: "no_instant"
        val startDate = context.period.startDate ?: "no_startdate"
        val endDate = context.period.endDate ?: "no_enddate"

        val explicitMembers =
            context.entity.segment?.explicitMembers?.sortedBy { it.dimension + it.value }?.joinToString()
                ?: "no_segment"

        return Hashing
            .sha256()
            .hashString(
                "$nodeName$entityId$documentPeriodEndDate$instant$startDate$endDate$explicitMembers",
                StandardCharsets.UTF_8
            )
            .toString()
    }

}
