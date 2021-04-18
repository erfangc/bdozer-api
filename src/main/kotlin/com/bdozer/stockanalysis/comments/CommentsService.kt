package com.bdozer.stockanalysis.comments

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service

@Service
class CommentsService(mongoDatabase: MongoDatabase) {

    val col = mongoDatabase.getCollection<Comment>()

    fun postComment(comment: Comment) {
        col.save(comment)
    }

    fun getComments(stockAnalysisId: String): List<Comment> {
        return col.find(Comment::stockAnalysisId eq stockAnalysisId).toList()
    }
}