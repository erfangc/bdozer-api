package com.starburst.starburst.edgar.filingentity.internal

data class SECEntityAddresses(
    val mailing: SECEntityAddress? = null,
    val business: SECEntityAddress? = null,
)