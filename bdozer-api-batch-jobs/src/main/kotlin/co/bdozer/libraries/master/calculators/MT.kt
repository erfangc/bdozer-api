package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.zacks.models.MT
import org.intellij.lang.annotations.Language

fun mt(ticker: String): MT {
    @Language("PostgreSQL") val result = Database.runSql(
        sql = """
        select * 
        from mt 
        where ticker = '$ticker'
    """.trimIndent(), MT::class
    ).first()

    println("Loaded master table for $ticker")
    return result
}