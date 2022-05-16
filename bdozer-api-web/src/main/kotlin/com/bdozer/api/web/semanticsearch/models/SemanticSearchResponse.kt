package com.bdozer.api.web.semanticsearch.models

import com.bdozer.api.web.semanticsearch.models.AnswerQuestionResponse

data class SemanticSearchResponse(val answer_candidates: List<AnswerQuestionResponse>)