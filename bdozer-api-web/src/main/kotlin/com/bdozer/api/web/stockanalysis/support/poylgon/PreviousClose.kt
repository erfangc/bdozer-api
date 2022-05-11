package com.bdozer.api.match.poylgon

data class PreviousClose(
    // Whether this response was adjusted for splits.
    val adjusted: Boolean? = null,
    // The exchange symbol that this item is traded under.
    val ticker: String?,
    // The number of aggregates (minute or day) used to generate the response.
    val queryCount: Int? = null,
    // A request id assigned by the server.
    val request_id: String?,
    // The total number of results for this request.
    val resultsCount: Int? = null,
    // The status of this request's response.
    val status: String?,
    val results: List<PreviouseCloseResult> = emptyList(),
)

