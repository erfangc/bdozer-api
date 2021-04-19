package com.bdozer.tag

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class TagService(mongoDatabase: MongoDatabase) {

    private val col = mongoDatabase.getCollection<Tag>()

    fun findTag(term: String): List<Tag> {
        return col
            .find()
            .filter { it._id.toLowerCase().contains(term.toLowerCase()) }
            .toList()
    }

    fun saveTag(tag: Tag) {
        col.save(tag)
    }

    fun deleteTag(id: String) {
        col.deleteOneById(id)
    }

}