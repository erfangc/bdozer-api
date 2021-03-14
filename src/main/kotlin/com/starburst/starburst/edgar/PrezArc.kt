//package com.starburst.starburst.edgar
//
//import com.starburst.starburst.edgar.XbrlConstants.link
//import com.starburst.starburst.edgar.XbrlConstants.xlink
//import com.starburst.starburst.edgar.filingentity.FilingEntityManager
//import com.starburst.starburst.edgar.utils.HttpClientExtensions.readXml
//import com.starburst.starburst.xml.XmlNode
//import org.apache.http.impl.client.HttpClientBuilder
//import java.net.URI
//import java.util.*
//
//fun main() {
//    printPre(root)
//}
//
///*
//Inputs
// */
//const val role = "http://www2.blackrock.com/20201231/taxonomy/role/StatementConsolidatedStatementsOfIncome"
//const val cik = "1364742"
//const val adsh = "000156459021008796"
//const val fileRoot = "blk-20201231_"
//const val root = "us-gaap_IncomeStatementAbstract"
//
//val prefix = "https://www.sec.gov/Archives/edgar/data/$cik/$adsh"
//val pre = "${prefix}/${fileRoot}pre.xml"
//val cal = "${prefix}/${fileRoot}cal.xml"
//
//private val http = HttpClientBuilder
//    .create()
//    .build()
//
//private fun printPre(root: String) {
//
//    val pre = http.readXml(pre)
//    val cal = http.readXml(cal)
//
//    val presentationLink = pre
//        .getElementsByTag(link, "presentationLink")
//        .find { it.role() == role }!!
//
//    val arcs = presentationLink
//        .getElementsByTag(link, "presentationArc")
//        .groupBy { it.from() }
//
//    val locs = presentationLink
//        .getElementsByTag(link, "loc")
//
//    val displayNames = locs
//        .associate {
//            it.label() to it.href()?.fragment
//        }
//
//    /*
//    calc shit
//     */
//    val calLink = cal
//        .getElementsByTag(link, "calculationLink")
//        .find { it.role() == role }!!
//    val calLocs = calLink
//        .getElementsByTag(link, "loc")
//        .associate { it.label() to it.href()?.fragment }
//    val calculations = calLink
//        .getElementsByTag(link, "calculationArc")
//        .groupBy { it.from() }
//        .entries
//        .associate {
//            (from, nodes) ->
//            calLocs[from] to nodes.map { calLocs[it.to()] }
//        }
//    /*
//    end of calc shit
//     */
//
//    val rootLoc = locs
//        .find { it.href()?.fragment == root }
//        .label()
//
//    /**
//     * [arcs]      - arcs
//     * [rootLoc]   - rootLoc
//     * [displayNames]  - locators
//     */
//    fun printArcs(
//        displayNames: Map<String?, String?>,
//        arcs: Map<String?, List<XmlNode>>,
//        rootLoc: String
//    ) {
//        val stack = Stack<String>()
//        stack.add(rootLoc)
//
//        val lastSibling = Stack<String>()
//        lastSibling.add(rootLoc)
//
//        fun print(msg: String?) {
//            val ident = (0 until lastSibling.size - 1).joinToString("") { "\t\t" }
//            println("$ident$msg")
//        }
//
//        while (stack.isNotEmpty()) {
//            val node = stack.pop()
//            val displayName = displayNames[node]
//            val msg = "$displayName = [${calculations[displayName]?.joinToString() ?: ""}]"
//            print(msg)
//            if (lastSibling.peek() == node) {
//                lastSibling.pop()
//            }
//            val children = arcs[node]
//
//            if (!children.isNullOrEmpty()) {
//                val elements = children.map { it.attr(xlink, "to") }.reversed()
//                stack.addAll(elements)
//                lastSibling.add(elements.first())
//            }
//        }
//    }
//
//    printArcs(
//        displayNames, arcs, rootLoc ?: error("...")
//    )
//}
//
//fun XmlNode?.label(): String? {
//    return this?.attr(xlink, "label")
//}
//
//fun XmlNode?.href(): URI? {
//    return this?.attr(xlink, "href")?.let { URI(it) }
//}
//
//fun XmlNode?.role(): String? {
//    return this?.attr(xlink, "role")
//}
//
//fun XmlNode?.from(): String? {
//    return this?.attr(xlink, "from")
//}
//
//fun XmlNode?.to(): String? {
//    return this?.attr(xlink, "to")
//}
