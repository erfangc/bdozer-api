package com.starburst.starburst.edgar.factbase.filingentity.internal

data class SECEntityAddresses(
    val mailing: SECEntityAddress? = null,
    val business: SECEntityAddress? = null,
)