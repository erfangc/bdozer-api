package co.bdozer.libraries.zacks

import SHRS
import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database
import co.bdozer.libraries.utils.Database.runSql
import co.bdozer.libraries.zacks.models.Table
import co.bdozer.libraries.zacks.models.ZacksDataMetadata
import co.bdozer.libraries.zacks.models.raw.FC
import co.bdozer.libraries.zacks.models.raw.FR
import co.bdozer.libraries.zacks.models.raw.MKTV
import co.bdozer.libraries.zacks.models.raw.MT
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.typeOf
import kotlin.system.exitProcess


object ZacksProcessorDriver {

    private val httpClient = Beans.httpClient()
    private val objectMapper = Beans.objectMapper()
    private val conn = Database.connection
    private val log = LoggerFactory.getLogger(ZacksProcessorDriver::class.java)
    fun processAllDatasets() {
        for (dataset in datasetMap.keys) {
            try {
                processDataset(dataset)
            } catch (e: Exception) {
                log.error("Caught exception while processing dataset={}", dataset, e)
            }
        }
    }
    fun processDataset(dataset: String) {
        val apiKey = System.getenv("QUANDL_API_KEY") ?: error("QUANDL_API_KEY required")
        val httpResponse = httpClient.send(
            HttpRequest
                .newBuilder(URI.create("https://www.quandl.com/api/v3/datatables/ZACKS/$dataset/delta.json?api_key=$apiKey"))
                .GET()
                .build(),
            BodyHandlers.ofInputStream(),
        )
        val body = httpResponse.body()
        val zacksDataMetadata = objectMapper.readValue<ZacksDataMetadata>(body)
        val latestIngestTime = latestIngestTime(dataset)
        if (latestIngestTime == null) {
            log.info("No prior runs found, ingesting everything dataset={}", dataset)
            // ingest everything
            ingestFullDataSet(zacksDataMetadata, dataset)
        } else {
            // ingest only the latest delta
            val filesToIngest = zacksDataMetadata
                .data
                .files
                .sortedByDescending { file -> file.to.localDateTime() }
                .takeWhile { file -> file.from.localDateTime()?.isAfter(latestIngestTime) == true }
                .sortedBy { file -> file.to.localDateTime() }
            log.info(
                "Found {} files to ingest, latestIngestTime={}, filesToIngest=[{}]",
                filesToIngest.size,
                latestIngestTime,
                filesToIngest.joinToString(",") { it.to.localDateTime().toString() }
            )
            for (file in filesToIngest) {
                log.info("Ingesting file [${file.from} -> ${file.to}] dataset={}", dataset)
                val clazz = datasetMap[dataset] ?: error("...")
                ZacksFileProcessor.processFile(link = file.insertions, clazz)
                updateMetadata(dataset, file.to.localDateTime()!!)
            }
        }
    }

    private fun latestIngestTime(dataset: String): LocalDateTime? {
        return runSql(
            """
            select last_processed_timestamp from zacks_metadata where dataset = '$dataset';
        """.trimIndent()
        ).firstOrNull()?.let { (it["last_processed_timestamp"] as Timestamp).toLocalDateTime() }
    }

    private val datasetMap = mapOf(
        "FR" to FR::class,
        "FC" to FC::class,
        "MKTV" to MKTV::class,
        "MT" to MT::class,
        "SHRS" to SHRS::class,
    )

    private fun ingestFullDataSet(
        zacksDataMetadata: ZacksDataMetadata,
        dataset: String
    ) {
        val clazz = datasetMap[dataset] ?: error("...")
        val tableName = clazz.findAnnotation<Table>()?.name?.ifEmpty { dataset.lowercase() } ?: dataset.lowercase()
        val stmt = conn.prepareStatement("truncate table $tableName")
        stmt.execute()
        val latestFullData = zacksDataMetadata.data.latest_full_data
        val link = latestFullData.full_data
        ZacksFileProcessor.processFile(link = link, clazz)
        updateMetadata(dataset, latestFullData.to.localDateTime()!!)
    }

    private fun updateMetadata(dataset: String, lastProcessedTimestamp: LocalDateTime) {
        val stmt = conn.prepareStatement(
            """
            insert into zacks_metadata (dataset, last_processed_timestamp) values (?, ?)
            on conflict on constraint zacks_metadata_pkey
            do update set last_processed_timestamp = ?
        """.trimIndent()
        )
        stmt.setString(1, dataset)
        stmt.setTimestamp(2, Timestamp.valueOf(lastProcessedTimestamp))
        stmt.setTimestamp(3, Timestamp.valueOf(lastProcessedTimestamp))
        stmt.execute()
    }
}

object ZacksFileProcessor {

    private val log = LoggerFactory.getLogger(ZacksFileProcessor::class.java)
    private val httpClient = Beans.httpClient()
    private val objectMapper = Beans.objectMapper()
    private val conn = Database.connection

    init {
        Runtime
            .getRuntime()
            .addShutdownHook(Thread { conn.close() })
    }

    fun <T : Any> processFile(link: String, clazz: KClass<T>) {
        log.info("Download link=$link")
        val httpResponse = httpClient.send(
            HttpRequest.newBuilder(URI.create(link)).build(),
            BodyHandlers.ofInputStream(),
        )
        log.info("Response headers=${httpResponse.headers()}")
        val inputStream = httpResponse.body()
        val filename = "./${clazz.simpleName}.zip"
        if (File(filename).exists()) {
            File(filename).delete()
        }
        inputStream.use { input ->
            Files.copy(input, Paths.get(filename))
        }
        log.info("Saved file to $filename")
        val zipInputStream = ZipInputStream(FileInputStream(filename))

        zipInputStream.nextEntry?.let { zipEntry ->
            log.info("Processing file ${zipEntry.name} size=${zipEntry.size}")
            var headers = emptyList<String>()
            var count = 0
            val ingestor = Ingestor(clazz)
            zipInputStream.bufferedReader().lines().forEach { line ->
                count++
                if (count == 1) {
                    // process header
                    headers = line.split(",")
                } else {
                    // process actual rows
                    // TODO convert this to a type safe object
                    val schema = clazz
                        .declaredMemberProperties
                        .associate { it.name to it.returnType }
                    val row: Map<String, Any?> = headers
                        .zip(tokenize(line))
                        .associate { (key, value) ->
                            val value: Any? = when (schema[key]) {
                                typeOf<Double>() -> value.toDoubleOrNull()
                                typeOf<Int>() -> value.toIntOrNull()
                                typeOf<Float>() -> value.toFloatOrNull()
                                typeOf<Long>() -> value.toLongOrNull()
                                typeOf<LocalDate>() -> LocalDate.parse(value)
                                else -> value
                            }
                            key to value
                        }
                    val item = objectMapper.treeToValue(
                        objectMapper.valueToTree(row),
                        clazz.java,
                    )
                    ingestor.ingest(item)
                }
            }
            ingestor.flushBuffer()

            // flush out the remaining items buffer
            log.info("Processed file ${zipEntry.name} total ${count - 1} rows")
            zipInputStream.close()
        }
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