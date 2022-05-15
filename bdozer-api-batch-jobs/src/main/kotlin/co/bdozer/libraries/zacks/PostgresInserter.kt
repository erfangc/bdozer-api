package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.zacks.models.Table
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.Types
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.typeOf

class PostgresInserter(clazz: KClass<Any>) {

    private val log = LoggerFactory.getLogger(PostgresInserter::class.java)
    private val conn = Database.connection
    private val properties = clazz.declaredMemberProperties
    private val className = clazz.simpleName
    private val tableName =
        clazz.findAnnotation<Table>()?.name?.let { it.ifEmpty { null } }
            ?: clazz.simpleName?.lowercase()
    private val buffer: MutableList<Any> = mutableListOf()
    private var total: Int = 0
    private val insertSql = """
        INSERT INTO $tableName (${properties.joinToString(", ") { it.name }})
        VALUES (${properties.joinToString(", ") { "?" }})
    """.trimIndent()

    fun insert(obj: Any) {
        buffer.add(obj)
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
        val stmt = conn.prepareStatement(insertSql)
        buffer.forEach { row ->
            properties.forEachIndexed { index, kProperty ->
                val parameterIdx = index + 1
                val value = kProperty.getValue(row, kProperty)
                val type = kProperty.returnType
                if (type == typeOf<Double>()) {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.DOUBLE)
                    } else {
                        stmt.setDouble(parameterIdx, value as Double)
                    }
                } else if (type == typeOf<Int>()) {
                    if (value == null) {
                        stmt.setNull(parameterIdx, Types.INTEGER)
                    } else {
                        stmt.setInt(parameterIdx, value as Int)
                    }
                } else if (type == typeOf<LocalDate>()) {
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
        log.info(
            "Postgres commit finished total={} buffer={} tableName={} className={}",
            total,
            buffer.size,
            tableName,
            className
        )
        buffer.clear()
    }
}