package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Beans
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory

class ElasticsearchIngestor(private val table: String) {

    private val log = LoggerFactory.getLogger(ElasticsearchIngestor::class.java)
    private var total: Int = 0
    private val buffer: MutableList<Pair<List<String>, List<Any?>>> = mutableListOf()
    private val objectMapper = Beans.objectMapper()
    private val restHighLevelClient = Beans.restHighLevelClient()

    fun ingest(row: Pair<List<String>, List<Any?>>) {
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
            val keys = item.first
            val values = item.second
            val map = keys.zip(values).toMap()
            bulkRequest.add(
                IndexRequest(table)
                    .source(
                        objectMapper.writeValueAsString(map),
                        XContentType.JSON,
                    )
            )
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT)
        log.info("Elasticsearch commit finished total={} buffer={} table={}", total, buffer.size, table)
        buffer.clear()
    }

}