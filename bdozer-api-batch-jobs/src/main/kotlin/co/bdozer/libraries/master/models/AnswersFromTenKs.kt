package co.bdozer.libraries.master.models

import java.time.LocalDate

data class AnswersFromTenKs(
    val url: String,
    val reportDate: LocalDate,
    val ash: String?,
    val answers: List<Answer>,
)