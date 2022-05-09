package co.bdozer.libraries.tenk.models

data class Recent(
    val acceptanceDateTime: List<String>? = null,
    val accessionNumber: List<String>? = null,
    val act: List<String>? = null,
    val fileNumber: List<String>? = null,
    val filingDate: List<String>? = null,
    val filmNumber: List<String>? = null,
    val form: List<String>? = null,
    val isInlineXBRL: List<Int>? = null,
    val isXBRL: List<Int>? = null,
    val items: List<String>? = null,
    val primaryDocDescription: List<String>? = null,
    val primaryDocument: List<String>? = null,
    val reportDate: List<String>? = null,
    val size: List<Int>? = null,
)