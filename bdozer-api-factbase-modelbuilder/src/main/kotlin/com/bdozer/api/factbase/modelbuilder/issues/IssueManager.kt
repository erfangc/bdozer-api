package com.bdozer.api.factbase.modelbuilder.issues

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.replaceOne

class IssueManager(mongoDatabase: MongoDatabase) {

    val issues = mongoDatabase.getCollection<Issue>()

    fun findIssues(stockAnalysisId: String): List<Issue> {
        return issues.find(Issue::stockAnalysisId eq stockAnalysisId).toList()
    }

    fun saveIssues(documents: List<Issue>) {
        if (documents.isEmpty()) {
            return
        }
        val bulk = documents.map { replaceOne(Issue::_id eq it._id, it, ReplaceOptions().upsert(true)) }
        issues.bulkWrite(bulk)
    }

    fun deleteIssue(id: String) {
        issues.deleteOneById(id)
    }

}