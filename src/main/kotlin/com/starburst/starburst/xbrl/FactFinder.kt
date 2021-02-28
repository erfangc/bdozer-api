package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.dataclasses.*
import com.starburst.starburst.xbrl.utils.NodeListExtension.findAllByTag
import com.starburst.starburst.xbrl.utils.NodeListExtension.findByTag
import com.starburst.starburst.xbrl.utils.NodeListExtension.toList
import org.w3c.dom.Node
import java.io.InputStream
import java.time.LocalDate

/**
 * [FactFinder]
 */
class FactFinder(instanceStream: InputStream) {

    private val root = XbrlUtils
        .readXml(instanceStream)
        .childNodes
        .toList()
        .groupBy { it.nodeName }

    private fun toContext(node: Node): XbrlContext {
        if (node.nodeName != "context")
            error("nodeNode must be context")

        val period = node.findByTag("period")
        val entity = node.findByTag("entity")
        val identifier = entity?.findByTag("identifier")

        val explicitMembers = entity
            ?.findByTag("segment")
            ?.findAllByTag("xbrldi:explicitMember")
            ?.map { myNode ->
                XbrlExplicitMember(
                    dimension = myNode.attributes.getNamedItem("dimension").textContent,
                    value = myNode.textContent
                )
            }

        return XbrlContext(
            id = node.attributes.getNamedItem("id").textContent,
            entity = XbrlEntity(
                identifier = XbrlIdentifier(
                    scheme = identifier?.attributes?.getNamedItem("scheme")?.textContent,
                    value = identifier?.textContent
                ),
                segment = explicitMembers?.let {
                    XbrlSegment(
                        explicitMembers = explicitMembers
                    )
                }
            ),
            period = XbrlPeriod(
                instant = period?.findByTag("instant")?.toLocalDate(),
                startDate = period?.findByTag("startDate")?.toLocalDate(),
                endDate = period?.findByTag("endDate")?.toLocalDate(),
            )
        )

    }

    private fun Node.toLocalDate(): LocalDate? {
        return LocalDate.parse(this.textContent)
    }

    fun get(name: String, namespace: String): Double? {
        return getString(name, namespace)?.toDoubleOrNull()
    }

    fun getString(name: String, namespace: String): String? {
        val tag = "$namespace:$name"

        val context = root["context"]
            ?.map { ctxNode ->
                toContext(ctxNode)
            }
            ?.associateBy { it.id } ?: error("no context found in file")

        //
        // determine the correct value based on
        //
        val noSegmentNodes = root[tag]?.map { node ->
            val contextRef = node.attributes.getNamedItem("contextRef").textContent
            node to context[contextRef]
        }?.filter { (node, context) ->
            context?.entity?.segment == null
        }?.sortedByDescending {
            val period = it.second?.period
            period?.endDate ?: period?.instant
        }
        return noSegmentNodes?.first()?.first?.textContent
    }
}
