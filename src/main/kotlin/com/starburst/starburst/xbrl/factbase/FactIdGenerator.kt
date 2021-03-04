package com.starburst.starburst.xbrl.factbase

import com.starburst.starburst.xbrl.dataclasses.XbrlContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets


class FactIdGenerator(instanceDocument: Element) {
    fun generateId(node: Node, context: XbrlContext): String {
        val nodeName = node.nodeName
        val entityId = context.entity.identifier.value
        val instant = context.period.instant ?: "no_instant"
        val startDate = context.period.startDate ?: "no_startdate"
        val endDate = context.period.endDate ?: "no_enddate"
        val explicitMembers =
            context.entity.segment?.explicitMembers?.sortedBy { it.dimension + it.value }?.joinToString()
                ?: "no_segment"
        return Hashing.sha256()
            .hashString("$nodeName$entityId$instant$startDate$endDate$explicitMembers", StandardCharsets.UTF_8)
            .toString()
    }
}
