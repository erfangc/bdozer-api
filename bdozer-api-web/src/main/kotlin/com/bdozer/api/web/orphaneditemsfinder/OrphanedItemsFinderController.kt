package com.bdozer.api.web.orphaneditemsfinder

import com.bdozer.api.models.dataclasses.Item
import com.bdozer.api.stockanalysis.dataclasses.StockAnalysis2
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