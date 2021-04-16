package com.bdozer.stockanalyzer.analyzers.support.itemgenerator

import com.bdozer.edgar.dataclasses.Concept
import com.bdozer.edgar.dataclasses.XbrlExplicitMember
import com.bdozer.edgar.factbase.core.FactBase
import com.bdozer.edgar.factbase.FactExtensions.filterForDimension
import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.edgar.factbase.dataclasses.DocumentFiscalPeriodFocus
import com.bdozer.edgar.factbase.dataclasses.Fact
import com.bdozer.edgar.factbase.ingestor.InstanceDocumentExtensions.documentPeriodEndDate
import com.bdozer.edgar.factbase.dataclasses.Arc
import com.bdozer.models.dataclasses.HistoricalValue
import com.bdozer.models.dataclasses.Item
import com.bdozer.models.dataclasses.ItemType
import com.bdozer.stockanalyzer.analyzers.extensions.General.conceptNotFound
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * # Overview
 * [ItemGenerator] is responsible for turning a single presentation Arc
 * into one or more [Item] instances. A presentation arc typically represents a single concept.
 *
 * ## Simple translation
 * In the absence of dimension declarations, the Arc could be thought of as having a soft 1-to-1 relationship with an [Item]
 * The generated item is backed by a dimensionless [Fact]
 *
 * ## When dimensions are declared
 * When an arc defines a concept that must be decomposed into declared dimensions - then the arc
 * is translated into multiple [Item] each backed by their own [Fact]
 *
 * ## What else must this class do
 *
 *  - Create a consistent item naming convention so that items can be reliable and reproducibly generated
 *  - Generated [Item] instances with calculations defined must have those calculations populated
 */
@Service
class ItemGenerator(val factBase: FactBase) {

    /**
     * Performs the primary tax of turning an [Arc]
     *
     * @param arc the [Arc] to be turned into [Item]
     * @param ctx the [GenerateItemContext] that provides the rest of the distilled formula from the filings as well as metadata
     * needed to perform the analysis
     */
    fun generateItems(arc: Arc, ctx: GenerateItemContext): List<Item> {

        /*
        Define and assign some variables we expect to use often
         */
        val conceptManager = ctx.filingProvider.conceptManager()
        val cik = ctx.filingProvider.cik()
        val dimensions = ctx.dimensions
        val concept = conceptManager.getConcept(arc.conceptHref) ?: conceptNotFound(arc.conceptHref)
        val documentPeriodEndDate = ctx.filingProvider.instanceDocument().documentPeriodEndDate() ?: error("...")

        /*
        Step 1 - query all facts for the concept defined by this arc
         */
        val facts = facts(cik, concept, documentPeriodEndDate)

        /*
        Step 2 - find the facts that we will itemize associated with this Arc's concept
        this is either the dimensionless item or we've split it up by some dimension
         */
        // TODO really tighten up the logic here (how do we know what are the correct combinations of dimensions to use)
        val dimension = dimensions.find { dimension ->
            /*
            choose the current dimension if there are facts matching all of its members
             */
            val filtered = facts.filter { fact -> explicitMembersMatchDimension(fact.explicitMembers, dimension) }
            filtered.isNotEmpty()
        }

        val itemizableFacts = if (dimension == null) {
            facts.find { it.explicitMembers.isEmpty() }?.let { listOf(it) } ?: return emptyList()
        } else {
            facts.filterForDimension(dimension)
        }

        /*
        Case 1 - only dimensionless fact exist
        Case 2 - both dimensionless and dimensional facts exist
        Case 3 - only dimensional facts exist
        Case 4 - neither exist
         */
        if (itemizableFacts.isEmpty()) {
            return emptyList()
        }

        /*
        Step 3 - Itemize all the itemizable facts
         */
        itemizableFacts.map { fact ->
            // FIXME determine if the item is a subtotal
            val subtotal = true
            val itemName = ItemNameGenerator().itemName(fact)
            Item(
                name = itemName,
                description = "",
                type = ItemType.Custom,
                historicalValue = HistoricalValue(),
                formula = "0.0",
                subtotal = subtotal,
            )
        }
        TODO()
    }

    /**
     * Determine if the passed in [XbrlExplicitMember]s match
     */
    private fun explicitMembersMatchDimension(
        explicitMembers: List<XbrlExplicitMember>,
        dimension: Dimension
    ): Boolean {
        return if (explicitMembers.size != 1) {
            false
        } else {
            val explicitMember = explicitMembers.first()
            (explicitMember.dimension == dimension.dimensionConcept
                    && dimension.memberConcepts.contains(explicitMember.value))
        }
    }

    /**
     * Get the latest facts for a given concept for a given filer
     */
    private fun facts(
        cik: String,
        concept: Concept,
        documentPeriodEndDate: LocalDate,
    ): List<Fact> {
        return factBase.getFacts(
            cik = cik,
            documentFiscalPeriodFocus = DocumentFiscalPeriodFocus.FY,
            documentPeriodEndDate = documentPeriodEndDate,
            conceptName = concept.conceptName,
        )
    }

}