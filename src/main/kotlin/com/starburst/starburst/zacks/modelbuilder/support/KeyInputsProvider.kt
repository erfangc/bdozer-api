package com.starburst.starburst.zacks.modelbuilder.support

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.zacks.dataclasses.KeyInputs
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class KeyInputsProvider(mongoDatabase: MongoDatabase) {

    val col = mongoDatabase.getCollection<KeyInputs>()

    fun getKeyInputs(ticker: String): KeyInputs? {
        return col.findOneById(ticker)
    }

    fun saveKeyInputs(keyInputs: KeyInputs) {
        col.save(keyInputs)
    }

}
