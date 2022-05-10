package co.bdozer.libraries.zacks

class Ingestor(table: String, schema: Map<String, String>) {
    
    private val elasticsearchIngestor = ElasticsearchIngestor(table)
    private val postgresIngestor = PostgresIngestor(table, schema)
    
    fun ingest(row: Pair<List<String>, List<Any?>>) {
         elasticsearchIngestor.ingest(row)
        postgresIngestor.ingest(row)
    }
    
    fun flushBuffer() {
        elasticsearchIngestor.flushBuffer()
        postgresIngestor.flushBuffer()
    }
    
}

