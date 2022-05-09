package co.bdozer.libraries.utils

import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

object Database {

    private val jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    private val jdbcUsername = System.getenv("JDBC_USERNAME") ?: "postgres"
    private val jdbcPassword = System.getenv("JDBC_PASSWORD")
    val connection: Connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword)

    fun <T : Any> runSql(sql: String, clazz: KClass<T>): Sequence<T> {
        return runSql(sql) { resultSet ->
            val ctor = clazz.constructors.first()
            val args = ctor.parameters.map { param ->
                val type = param.type
                val name = param.name
                val value = when (type) {
                    typeOf<Double>() -> resultSet.getDouble(name)
                    typeOf<Double?>() -> resultSet.getDouble(name)
                    typeOf<Instant>() -> resultSet.getTimestamp(name).toInstant()
                    typeOf<Instant?>() -> resultSet.getTimestamp(name)?.toInstant()
                    typeOf<Int>() -> resultSet.getInt(name)
                    typeOf<Int?>() -> resultSet.getInt(name)
                    typeOf<Long>() -> resultSet.getLong(name)
                    typeOf<Long?>() -> resultSet.getLong(name)
                    typeOf<String>() -> resultSet.getString(name)
                    typeOf<String?>() -> resultSet.getString(name)
                    typeOf<Float>() -> resultSet.getFloat(name)
                    typeOf<Float?>() -> resultSet.getFloat(name)
                    typeOf<BigDecimal>() -> resultSet.getBigDecimal(name)
                    typeOf<BigDecimal?>() -> resultSet.getBigDecimal(name)
                    typeOf<LocalDate>() -> resultSet.getDate(name).toLocalDate()
                    typeOf<LocalDate?>() -> resultSet.getDate(name)?.toLocalDate()
                    typeOf<Boolean>() -> resultSet.getBoolean(name)
                    typeOf<Boolean?>() -> resultSet.getBoolean(name)
                    else -> null
                }
                if (value == null && !type.isMarkedNullable) {
                    error("property $name on class ${clazz.simpleName} is not nullable, but was null on SQL ResultSet")
                } else {
                    value
                }
            }.toTypedArray()
            ctor.call(*args)
        }
    }

    fun runSql(sql: String): Sequence<Map<String, Any?>> {
        return runSql(sql) { resultSet ->
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount

            (1..columnCount).associate { columnIdx ->
                val columnType = metaData.getColumnType(columnIdx)
                val columnName = metaData.getColumnName(columnIdx)

                val columnValue: Any? = when (columnType) {
                    Types.BINARY -> resultSet.getBlob(columnIdx)
                    Types.VARCHAR -> resultSet.getString(columnIdx)
                    Types.NUMERIC -> resultSet.getDouble(columnIdx)
                    Types.DOUBLE -> resultSet.getDouble(columnIdx)
                    Types.FLOAT -> resultSet.getDouble(columnIdx)
                    Types.DECIMAL -> resultSet.getDouble(columnIdx)
                    Types.INTEGER -> resultSet.getInt(columnIdx)
                    Types.BIGINT -> resultSet.getLong(columnIdx)
                    Types.SMALLINT -> resultSet.getInt(columnIdx)
                    Types.DATE -> resultSet.getDate(columnIdx)
                    Types.TIMESTAMP -> resultSet.getTimestamp(columnIdx)
                    Types.TIME -> resultSet.getTime(columnIdx)
                    Types.CHAR -> resultSet.getString(columnIdx)
                    else -> resultSet.getString(columnCount)
                }
                columnName to columnValue
            }
        }
    }

    fun <T> runSql(sql: String, rowMapper: (resultSet: ResultSet) -> T): Sequence<T> {
        val stmt = connection.createStatement()
        val resultSet = stmt.executeQuery(sql)
        return generateSequence {
            if (resultSet.next()) {
                rowMapper.invoke(resultSet)
            } else {
                null
            }
        }
    }
}

