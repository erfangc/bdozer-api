package com.starburst.starburst.edgar.explorer.dataclasses

data class EdgarEntity(
    val _id: String? = null,
    val _index: String? = null,
    val _type: String? = null,
    val _source: EdgarEntitySource,
)