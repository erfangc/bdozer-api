package co.bdozer.libraries.master.calculators

import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.zacks.models.FC
import org.intellij.lang.annotations.Language

data class FCS(
    val annuals: List<FC>,
    val quarters: List<FC>,
)

fun fcs(ticker: String): FCS {
    @Language("PostgreSQL") val results = Database.runSql(
        sql = """
            select *
            from fc
            where ticker = '$ticker' 
            order by qtr_nbr desc 
            """.trimIndent(), FC::class
    ).toList()

    val groupBy = results.groupBy { it.per_type }
    val annuals = groupBy["A"]?.take(2) ?: emptyList()
    val quarters = groupBy["Q"]?.take(6) ?: emptyList()

    println("Loaded fundamentals characteristics for $ticker")
    return FCS(annuals = annuals, quarters = quarters)
}