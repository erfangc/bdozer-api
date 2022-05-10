package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Database
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.Types

class PostgresIngestor(
    private val table: String,
    private val schema: Map<String, String>
) {

    private val log = LoggerFactory.getLogger(PostgresIngestor::class.java)
    private var total: Int = 0
    private val buffer: MutableList<Pair<List<String>, List<Any?>>> = mutableListOf()
    private val conn = Database.connection

    fun ingest(row: Pair<List<String>, List<Any?>>) {
        buffer.add(row)
        total++
        if (buffer.size >= 1000)
            flushBuffer()
    }

    fun flushBuffer() {
        if (buffer.isEmpty()) {
            return
        }
        val autoCommit = conn.autoCommit
        conn.autoCommit = false
        val keys = buffer.first().first
        val stmt = conn.prepareStatement(
            "insert into $table (${keys.joinToString(",")}) values (${keys.joinToString(", ") { "?" }})"
        )
        buffer.forEach { (keys, values) ->
            keys.zip(values).forEachIndexed { idx, (key, value) ->
                val parameterIdx = idx + 1
                val type = schema[key] ?: error("...")
                if (type == "numeric") {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.DOUBLE)
                    } else {
                        stmt.setDouble(parameterIdx, value as Double)
                    }
                } else if (type.startsWith("int")) {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.INTEGER)
                    } else {
                        stmt.setInt(parameterIdx, value as Int)
                    }
                } else if (type == "date") {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.DATE)
                    } else {
                        stmt.setDate(parameterIdx, Date.valueOf(value.toString()))
                    }
                } else {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.VARCHAR)
                    } else {
                        stmt.setString(parameterIdx, value.toString())
                    }
                }
            }
            stmt.addBatch()
        }
        stmt.executeBatch()
        conn.commit()
        conn.autoCommit = autoCommit
        log.info("Postgres commit finished total={} buffer={} table={}", total, buffer.size, table)
        buffer.clear()
    }
}