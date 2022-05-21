package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.zacks.models.PrimaryKeyComponent
import co.bdozer.libraries.zacks.models.Table
import org.slf4j.LoggerFactory
import java.sql.Date
import java.sql.Types
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.typeOf

class PostgresInserter<T : Any>(clazz: KClass<T>) {

    private val log = LoggerFactory.getLogger(PostgresInserter::class.java)
    private val conn = Database.connection
    private val className = clazz.simpleName
    private val tableName =
        clazz.findAnnotation<Table>()?.name?.let { it.ifEmpty { null } }
            ?: clazz.simpleName?.lowercase()

    private val properties = clazz.declaredMemberProperties
    /*
    keyProperties - properties that consists of the primary key
     */
    private val keyProperties = clazz
        .declaredMemberProperties
        .filter { property ->
            property.hasAnnotation<PrimaryKeyComponent>()
        }
        .sortedByDescending { it.name }

    /*
    valueProperties - properties that consists of the non-key values
     */
    private val valueProperties = clazz
        .declaredMemberProperties
        .filter { property ->
            !property.hasAnnotation<PrimaryKeyComponent>()
        }
        .sortedByDescending { it.name }
    
    private val upsertSql = """
        insert into $tableName (${keyProperties.joinToString(", ") { it.name }}, ${valueProperties.joinToString(", ") { it.name }}) 
        values (${keyProperties.joinToString(", ") { "?" }}, ${valueProperties.joinToString(", ") { "?" }})
        on conflict on constraint ${tableName}_pkey
        do update set ${valueProperties.joinToString(", "){ "${it.name} = ?" }}
    """.trimIndent()
    
    private fun indices(property: KProperty1<T, *>): List<Int> {
        val keySize = keyProperties.size
        val valueSize = valueProperties.size
        val keyIdx = keyProperties.indexOf(property)
        return if (keyIdx != -1) {
            listOf(keyIdx + 1)
        } else {
            val valueIdx = valueProperties.indexOf(property)
            listOf(keySize + valueIdx + 1, keySize + valueSize + valueIdx + 1)
        }
    }
    
    private val buffer: MutableList<T> = mutableListOf()
    private var total: Int = 0

    fun insert(obj: T) {
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
        val stmt = conn.prepareStatement(upsertSql)
        buffer.forEach { row ->
            properties.forEach { kProperty ->
                
                val parameterIndices = indices(kProperty)
                val value = kProperty.getValue(row, kProperty)
                val type = kProperty.returnType
                
                if (type == typeOf<Double?>()) {
                    if (value == null) {
                        parameterIndices.forEach { idx -> stmt.setNull(idx, Types.DOUBLE) }
                    } else {
                        parameterIndices.forEach { idx -> stmt.setDouble(idx, value as Double) }
                    }
                } else if (type == typeOf<Double>()) {
                    parameterIndices.forEach { idx -> stmt.setDouble(idx, value as Double) }
                } else if (type == typeOf<Float?>()) {
                    if (value == null) {
                        parameterIndices.forEach { idx -> stmt.setNull(idx, Types.NUMERIC) }
                    } else {
                        parameterIndices.forEach { idx -> stmt.setFloat(idx, value as Float) }
                    }
                } else if (type == typeOf<Float>()) {
                    parameterIndices.forEach { idx -> stmt.setFloat(idx, value as Float) }
                } else if (type == typeOf<Int?>()) {
                    if (value == null) {
                        parameterIndices.forEach { idx -> stmt.setNull(idx, Types.INTEGER) }
                    } else {
                        parameterIndices.forEach { idx -> stmt.setInt(idx, value as Int) }
                    }
                } else if (type == typeOf<Int>()) {
                    parameterIndices.forEach { idx -> stmt.setInt(idx, value as Int) }
                } else if (type == typeOf<LocalDate?>()) {
                    if (value == null) {
                        parameterIndices.forEach { idx -> stmt.setNull(idx, Types.DATE) }
                    } else {
                        parameterIndices.forEach { idx -> stmt.setDate(idx, Date.valueOf(value.toString())) }
                    }
                } else if (type == typeOf<LocalDate>()) {
                    parameterIndices.forEach { idx -> stmt.setDate(idx, Date.valueOf(value.toString())) }
                } else {
                    if (value == null) {
                        parameterIndices.forEach { idx -> stmt.setNull(idx, Types.VARCHAR) }
                    } else {
                        parameterIndices.forEach { idx -> stmt.setString(idx, value.toString()) }
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