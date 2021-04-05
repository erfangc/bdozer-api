package com.starburst.starburst.stockanalyzer.overrides

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class ModelOverrideService(mongoDatabase: MongoDatabase) {
    private val overrides = mongoDatabase.getCollection<ModelOverride>()

    fun getOverrides(cik: String): ModelOverride {
        val modelOverride = ModelOverride(
            _id = cik,
            cik = cik,
        )
        return overrides.findOneById(cik) ?: modelOverride
    }

    fun saveOverrides(body: ModelOverride) {
        overrides.save(body)
    }

}