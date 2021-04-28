package com.bdozer.revenuemodeler

import com.bdozer.revenuemodeler.dataclasses.RevenueModel
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

/**
 * [RevenueModeler] model revenues by creating a linear combination
 * of various driving factors
 */
@Service
class RevenueModeler(
    mongoDatabase: MongoDatabase
) {

    val col = mongoDatabase.getCollection<RevenueModel>()

    fun getRevenueModel(id: String): RevenueModel? {
        return col.findOneById(id)
    }

    fun deleteRevenueModel(id: String) {
        col.deleteOneById(id)
    }

    fun saveRevenueModel(revenueModel: RevenueModel) {
        return col.save(revenueModel)
    }

}