package co.bdozer.libraries.zacks

import kotlin.reflect.KClass

class Ingestor(clazz: KClass<Any>) {
    
    private val elasticsearchIngestor = ElasticsearchInserter(clazz)
    private val postgresIngestor = PostgresInserter(clazz)
    
    fun ingest(row: Any) {
         elasticsearchIngestor.insert(row)
        postgresIngestor.insert(row)
    }
    
    fun flushBuffer() {
        elasticsearchIngestor.flushBuffer()
        postgresIngestor.flushBuffer()
    }
    
}

