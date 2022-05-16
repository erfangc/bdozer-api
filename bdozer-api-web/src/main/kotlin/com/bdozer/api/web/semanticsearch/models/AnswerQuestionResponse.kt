package com.bdozer.api.web.semanticsearch.models

data class AnswerQuestionResponse(
    val answer_text: String,
    val answer_highlighted: String,
    val document: Document,
    val score: Double,
)