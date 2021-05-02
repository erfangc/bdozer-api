package com.bdozer.filingentity

import com.bdozer.filingentity.dataclasses.FilingEntity
import com.bdozer.sec.factbase.core.FactBase
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class FilingEntityManager(
    mongoDatabase: MongoDatabase,
    private val factBase: FactBase,
) {

    companion object {
        const val Completed = "Completed"
        const val Created = "Created"
        const val Bootstrapping = "Bootstrapping"
    }

    private val col = mongoDatabase.getCollection<FilingEntity>()

    fun getFilingEntity(cik: String): FilingEntity? {
        return col.findOneById(cik.padStart(10, '0'))
    }

    fun saveFilingEntity(filingEntity: FilingEntity) {
        col.save(filingEntity)
    }

    fun deleteFilingEntity(cik: String) {
        /*
        delete any existing data on this entity
         */
        col.deleteMany(FilingEntity::cik eq cik)
        factBase.deleteAll(cik)
    }

}
