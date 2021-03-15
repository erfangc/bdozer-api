package com.starburst.starburst.zacks.modelbuilder

import com.mongodb.client.MongoClient
import com.starburst.starburst.zacks.ZacksEstimatesService
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@ExperimentalStdlibApi
@Service
class KeyInputsProvider(
    val mongo:MongoClient,
    val zacksEstimatesService: ZacksEstimatesService,
) {

    val col = mongo.getDatabase("starburst").getCollection<KeyInputs>()

    fun getKeyInputs(ticker: String): KeyInputs {
        return col.findOneById(ticker) ?: bootstrapKeyInputsFromZacksSalesEstimates(ticker)
    }

    private fun bootstrapKeyInputsFromZacksSalesEstimates(ticker: String): KeyInputs {
        val zacksSaleEstimates = zacksEstimatesService.getZacksSaleEstimates(ticker)
        TODO("Not yet implemented")
    }

    fun saveKeyInputs(keyInputs: KeyInputs) {
        col.save(keyInputs)
    }

}
