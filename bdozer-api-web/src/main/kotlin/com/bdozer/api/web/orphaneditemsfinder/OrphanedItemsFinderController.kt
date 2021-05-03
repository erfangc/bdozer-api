package com.bdozer.api.web.orphaneditemsfinder

import bdozer.api.common.model.Item
import bdozer.api.common.stockanalysis.StockAnalysis2
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("api/orphaned-items-finder")
class OrphanedItemsFinderController {

    private val orphanedItemsFinder = OrphanedItemsFinder()

    @PostMapping
    fun orphanedItems(@RequestBody stockAnalysis2: StockAnalysis2): List<Item> {
        return orphanedItemsFinder.findOrphanedItems(stockAnalysis2)
    }
}