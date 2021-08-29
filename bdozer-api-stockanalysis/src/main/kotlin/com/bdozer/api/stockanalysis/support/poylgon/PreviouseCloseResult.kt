package com.bdozer.api.match.poylgon

data class PreviouseCloseResult(
    // The close price for the symbol in the given time period.
    val c: Double? = null,
    // The highest price for the symbol in the given time period.
    val h: Double? = null,
    // The lowest price for the symbol in the given time period.
    val l: Double? = null,
    // The number of transactions in the aggregate window.
    val n: Double? = null,
    // The open price for the symbol in the given time period.
    val o: Double? = null,
    // The Unix Msec timestamp for the start of the aggregate window.
    val t: Long? = null,
    // The trading volume of the symbol in the given time period.
    val v: Double? = null,
    // The volume weighted average price.
    val vw: Double? = null,
)