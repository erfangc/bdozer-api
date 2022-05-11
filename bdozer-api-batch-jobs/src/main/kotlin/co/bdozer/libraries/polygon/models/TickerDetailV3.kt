package co.bdozer.libraries.polygon.models

data class TickerDetailV3(
    val request_id: String,
    val results: Results = Results(),
    val status: String
)