package co.bdozer.libraries.master.models

data class Answer(
    val question: String,
    val bestMatchContext: String,
    val answer: String,
    val score: Double,
)