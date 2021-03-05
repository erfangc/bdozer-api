package com.starburst.starburst.edgar.old

import com.starburst.starburst.edgar.dataclasses.*
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementsByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.getElementByTag
import com.starburst.starburst.edgar.utils.NodeListExtension.toList
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.io.InputStream
import java.lang.Exception
import java.time.LocalDate

/**
 * [FactFinder]
 */
class FactFinder(instanceStream: InputStream) {

    private val log = LoggerFactory.getLogger(FactFinder::class.java)

    private val root = XbrlUtils
        .readXml(instanceStream)
        .childNodes
        .toList()
        .groupBy { it.nodeName }

    private fun toContext(node: Node): XbrlContext {
        if (node.nodeName != "context")
            error("nodeNode must be context")

        val period = node.getElementByTag("period")
        val entity = node.getElementByTag("entity")
        val identifier = entity?.getElementByTag("identifier")

        val explicitMembers = entity
            ?.getElementByTag("segment")
            ?.getElementsByTag("xbrldi:explicitMember")
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
                instant = period?.getElementByTag("instant")?.toLocalDate(),
                startDate = period?.getElementByTag("startDate")?.toLocalDate(),
                endDate = period?.getElementByTag("endDate")?.toLocalDate(),
            )
        )

    }

    fun Node.toLocalDate(): LocalDate? {
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
        return try {
            noSegmentNodes?.first()?.first?.textContent
        } catch (e: Exception) {
            log.info("Unable to determine the value for $namespace:$name")
            null
        }
    }
}
