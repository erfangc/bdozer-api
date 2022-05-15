package co.bdozer.libraries.zacks

import kotlin.reflect.KClass

class Ingestor<T : Any>(clazz: KClass<T>) {
    
    private val elasticsearchIngestor = ElasticsearchInserter(clazz)
    private val postgresIngestor = PostgresInserter(clazz)
    
    fun ingest(row: T) {
         elasticsearchIngestor.insert(row)
        postgresIngestor.insert(row)
    }
    
    fun flushBuffer() {
        elasticsearchIngestor.flushBuffer()
        postgresIngestor.flushBuffer()
    }
    
}

