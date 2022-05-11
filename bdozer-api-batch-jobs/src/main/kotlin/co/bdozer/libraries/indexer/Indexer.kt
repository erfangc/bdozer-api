package co.bdozer.libraries.indexer

import co.bdozer.libraries.utils.Beans
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.xcontent.XContentType

object Indexer {

    private val objectMapper = Beans.objectMapper()
    private val restHighLevelClient = Beans.restHighLevelClient()

    fun index(id: String, obj: Any): IndexResponse {
        val index = obj::class.java.simpleName.lowercase()
        return index(index, id, obj)
    }
    
    fun index(index:String, id: String, obj: Any): IndexResponse {
        val json = objectMapper.writeValueAsString(obj)
        return restHighLevelClient.index(
            IndexRequest(index)
                .id(id)
                .source(json, XContentType.JSON),
            RequestOptions.DEFAULT
        )
    }
}