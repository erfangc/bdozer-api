package co.bdozer.libraries.polygon.models

data class PreviousClose(
    val adjusted: Boolean,
    val queryCount: Int,
    val request_id: String,
    val results: List<Result>,
    val resultsCount: Int,
    val status: String,
    val ticker: String
)