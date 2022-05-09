package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.zacks.models.ZacksDownloadLink
import com.fasterxml.jackson.module.kotlin.readValue
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.zip.ZipInputStream


object ZacksTableSyncer {

    private val quandlApiKey = System.getenv("QUANDL_API_KEY") ?: error("QUANDL_API_KEY not defined")

    private val downloadLinks = mapOf(
        "fc" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/FC?api_key=$quandlApiKey&qopts.export=true"),
        "fr" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/FR?api_key=$quandlApiKey&qopts.export=true"),
        "mt" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/MT?api_key=$quandlApiKey&qopts.export=true"),
        "shrs" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/SHRS?api_key=$quandlApiKey&qopts.export=true"),
        "mktv" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/MKTV?api_key=$quandlApiKey&qopts.export=true"),
        "se" to URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/SE?api_key=$quandlApiKey&qopts.export=true"),
    )
    private val restHighLevelClient = Beans.restHighLevelClient()
    private val httpClient = Beans.httpClient()
    private val objectMapper = Beans.objectMapper()
    private val conn = Database.connection

    init {
        Runtime.getRuntime().addShutdownHook(Thread { conn.close() })
    }

    private fun schema(tbl: String): Map<String, String> {
        val resultSet = conn.createStatement().executeQuery(schemaSql(tbl))
        val sequence = generateSequence {
            if (resultSet.next()) {
                resultSet.getString("attname") to resultSet.getString("typname")
            } else {
                null
            }
        }
        return sequence.toMap()
    }

    private fun schemaSql(tbl: String) = """
    select pa.attname, pt.typname
    from pg_class c
             join pg_attribute pa on c.oid = pa.attrelid
    join pg_type pt on pa.atttypid = pt.oid
    where relname = '$tbl'
""".trimIndent()

    private fun zacksDownloadLink(uri: URI): ZacksDownloadLink {
        val bytes = httpClient.send(
            HttpRequest.newBuilder(uri).GET().header("user-agent", "curl/7.79.1").header("accept", "*/*").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        ).body()
        return objectMapper.readValue(bytes)
    }

    fun syncTables() {
        downloadLinks.keys.forEach { table -> syncTable(table) }
    }

    fun syncTable(table: String) {
        truncate(table)
        val uri = downloadLinks[table]!!
        val zacksDownloadLink = zacksDownloadLink(uri)
        val link = zacksDownloadLink.datatable_bulk_download.file.link
        println("Download $link")
        val httpResponse = httpClient.send(
            HttpRequest.newBuilder(URI.create(link)).build(),
            HttpResponse.BodyHandlers.ofInputStream(),
        )
        println("Response headers=${httpResponse.headers()}")
        val body = httpResponse.body()
        val zipInputStream = ZipInputStream(body)
        val schema = schema(table)
        
        zipInputStream.nextEntry?.let { zipEntry ->
            println("Processing file ${zipEntry.name} size=${zipEntry.size}")
            var headers = emptyList<String>()
            var count = 0

            val buffer = mutableListOf<Map<String, Any?>>()
            zipInputStream.bufferedReader().lines().forEach { line ->
                count++
                if (count == 1) {
                    // process header
                    headers = line.split(",")
                } else {
                    // process actual rows
                    val pairs = headers.zip(tokenize(line))
                    val keys = pairs.map { it.first }
                    val values: List<Any?> = pairs.map { (key, value) ->
                        if (schema[key] == "numeric") {
                            value.toDoubleOrNull()
                        } else if (schema[key]?.startsWith("int") == true) {
                            value.toIntOrNull()
                        } else if (value.isEmpty()) {
                            null
                        } else {
                            "'${value.replace("'", "''")}'"
                        }
                    }
                    val asMap = keys.zip(values).toMap()
                    buffer.add(asMap)
                    if (buffer.size >= 100) {
                        val bulkRequest = BulkRequest()
                        buffer.forEach { item ->
                            bulkRequest.add(IndexRequest(table).source(
                                objectMapper.writeValueAsString(item), XContentType.JSON
                            ))
                        }
                        restHighLevelClient.bulk(
                            bulkRequest,
                            RequestOptions.DEFAULT
                        )
                        println("Processed buffer=${buffer.size} $count rows to Elasticsearch")
                        buffer.clear()
                    }
                    val sql = """
                        insert into $table (${keys.joinToString(",")}) values (${values.joinToString(",")})
                        """.trimIndent()

                    try {
                        val smt = conn.createStatement()
                        smt.execute(sql)
                        println("Processed $count rows")
                    } catch (e: Exception) {
                        println("Error while executing SQL: $sql")
                        throw RuntimeException(e)
                    }
                }
            }
            
            // flush out the remaining items buffer
            if (buffer.isNotEmpty()) {
                val bulkRequest = BulkRequest()
                buffer.forEach { item ->
                    bulkRequest.add(IndexRequest(table).source(
                        objectMapper.writeValueAsString(item), XContentType.JSON
                    ))
                }
                restHighLevelClient.bulk(
                    bulkRequest,
                    RequestOptions.DEFAULT
                )
                println("Processed buffer=${buffer.size} $count rows to Elasticsearch")
                buffer.clear()
            }

            println("Processed file ${zipEntry.name} total $count rows")
            zipInputStream.close()
        }
    }

    private fun truncate(table: String) {
        if (restHighLevelClient.indices().exists(GetIndexRequest(table), RequestOptions.DEFAULT)) {
            println("Deleted index $table from Elasticsearch")
            restHighLevelClient.indices().delete(DeleteIndexRequest(table), RequestOptions.DEFAULT)
        }
        val stmt = conn.createStatement()
        stmt.execute("truncate table $table")
    }

    private fun tokenize(input: String): List<String> {
        val tokens: MutableList<String> = ArrayList()
        var startPosition = 0
        var isInQuotes = false
        for (currentPosition in input.indices) {
            if (input[currentPosition] == '\"') {
                isInQuotes = !isInQuotes
            } else if (input[currentPosition] == ',' && !isInQuotes) {
                tokens.add(input.substring(startPosition, currentPosition))
                startPosition = currentPosition + 1
            }
        }

        val lastToken: String = input.substring(startPosition)
        if (lastToken == ",") {
            tokens.add("")
        } else {
            tokens.add(lastToken)
        }
        return tokens
    }
}