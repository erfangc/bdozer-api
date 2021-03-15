package com.starburst.starburst.zacks.modelbuilder

import com.mongodb.client.MongoClient
import com.starburst.starburst.models.dataclasses.Discrete
import com.starburst.starburst.zacks.ZacksEstimatesService
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@ExperimentalStdlibApi
@Service
class KeyInputsProvider(
    val mongo: MongoClient,
    val zacksEstimatesService: ZacksEstimatesService,
) {

    val col = mongo.getDatabase("starburst").getCollection<KeyInputs>()

    fun getKeyInputs(ticker: String): KeyInputs {
        return col.findOneById(ticker) ?: bootstrapKeyInputsFromZacksSalesEstimates(ticker)
    }

    private fun bootstrapKeyInputsFromZacksSalesEstimates(ticker: String): KeyInputs {
        val zacksSaleEstimates = zacksEstimatesService
            .getZacksSaleEstimates(ticker)
            .filter { it.per_type == "A" }
            .sortedBy { it.per_fisc_year }

        /*
        turn sales estimates into a set of discrete
        inputs on [KeyInput]
         */
        val keyInputs = KeyInputs(
            _id = ticker,
            keyInputs = emptyList(),
            discrete = Discrete(
                formulas = zacksSaleEstimates
                    .filter { it.sales_mean_est != null }
                    .mapIndexed { idx, ests ->
                        idx + 1 to "${ests.sales_median_est}"
                    }.toMap(),
            )
        )

        saveKeyInputs(keyInputs)
        return keyInputs
    }

    fun saveKeyInputs(keyInputs: KeyInputs) {
        col.save(keyInputs)
    }

}
