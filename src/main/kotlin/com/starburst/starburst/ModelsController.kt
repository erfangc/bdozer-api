package com.starburst.starburst

import com.starburst.starburst.models.ReservedItemNames
import com.starburst.starburst.models.*
import com.starburst.starburst.models.builders.ModelBuilder
import com.starburst.starburst.models.builders.SkeletonModel.dropbox
import com.starburst.starburst.models.translator.subtypes.dataclasses.SubscriptionRevenue
import com.starburst.starburst.models.translator.subtypes.dataclasses.PercentOfRevenue
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("models")
class ModelsController(private val modelBuilder: ModelBuilder) {

    @GetMapping("default")
    fun default(): Model {
        return modelBuilder.reformulateModel(dropbox)
    }

}
