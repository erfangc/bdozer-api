package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.HashGenerator.hash
import co.bdozer.libraries.zacks.models.PrimaryKeyComponent
import co.bdozer.libraries.zacks.models.Table
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class ElasticsearchInserter(clazz: KClass<Any>) {

    private val log = LoggerFactory.getLogger(ElasticsearchInserter::class.java)
    private var total: Int = 0
    private val buffer: MutableList<Any> = mutableListOf()
    private val objectMapper = Beans.objectMapper()
    private val restHighLevelClient = Beans.restHighLevelClient()
    private val className = clazz.simpleName
    private val tableName = clazz.findAnnotation<Table>()
        ?.name
        ?.let { it.ifEmpty { null } }
        ?: clazz.simpleName?.lowercase()
    
    private val primaryKeyProperties = clazz
        .declaredMemberProperties
        .filter { property -> property.hasAnnotation<PrimaryKeyComponent>() }

    fun insert(row: Any) {
        buffer.add(row)
        total++
        if (buffer.size >= 150) {
            flushBuffer()
        }
    }

    fun flushBuffer() {
        if (buffer.isEmpty()) {
            return
        }
        val bulkRequest = BulkRequest()
        buffer.forEach { item ->
            val json = objectMapper.writeValueAsString(item)
            bulkRequest.add(
                IndexRequest(tableName)
                    // create id
                    .id(id(item))
                    .source(json,XContentType.JSON)
            )
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT)
        log.info(
            "Elasticsearch commit finished total={} buffer={} table={} className={}",
            total,
            buffer.size,
            tableName,
            className
        )
        buffer.clear()
    }

    private fun id(item: Any): String {
        val parts = primaryKeyProperties.map { property ->
            property.getValue(item, property).toString()
        }.toTypedArray()
        return hash(*parts)
    }

}