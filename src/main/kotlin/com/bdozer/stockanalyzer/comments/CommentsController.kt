package com.bdozer.stockanalyzer.comments

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/comments")
@CrossOrigin
class CommentsController(private val commentsService: CommentsService) {
    @PostMapping
    fun postComment(@RequestBody comment: Comment) {
        commentsService.postComment(comment)
    }

    @GetMapping("{stockAnalysisId}")
    fun getComments(@PathVariable stockAnalysisId: String): List<Comment> {
        return commentsService.getComments(stockAnalysisId)
    }
}