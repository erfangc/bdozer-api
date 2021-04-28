package com.bdozer.revenuemodeler

import com.bdozer.revenuemodeler.dataclasses.RevenueModel
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("api/revenue-modeler")
class RevenueModelerController(private val revenueModeler: RevenueModeler) {
    @GetMapping("{id}")
    fun getRevenueModel(@PathVariable id: String): RevenueModel? {
        return revenueModeler.getRevenueModel(id)
    }

    @DeleteMapping("{id}")
    fun deleteRevenueModel(@PathVariable id: String) {
        return revenueModeler.deleteRevenueModel(id)
    }

    @PostMapping
    fun saveRevenueModel(@RequestBody revenueModel: RevenueModel) {
        return revenueModeler.saveRevenueModel(revenueModel)
    }
}