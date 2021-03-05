package com.starburst.starburst.edgar.explorer

import java.time.LocalDate

data class EdgarFilingMetadata(
    val ciks: List<String> = emptyList(),
    val period_ending: LocalDate,
    val root_form: String? = null,
    val file_num: List<String> = emptyList(),
    val display_names: List<String> = emptyList(),
    val sequence: String? = null,
    val biz_states: List<String> = emptyList(),
    val sics: List<String> = emptyList(),
    val form: String,
    val adsh: String,
    val biz_locations: List<String> = emptyList(),
    val file_date: LocalDate,
    val file_type: String? = null,
    val file_description: String? = null,
    val inc_states: List<String> = emptyList()
)