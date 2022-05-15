package co.bdozer.libraries.zacks

import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.zip.ZipInputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.typeOf


object ZacksTableSyncer {

    private val log = LoggerFactory.getLogger(ZacksTableSyncer::class.java)
    private val httpClient = Beans.httpClient()
    private val objectMapper = Beans.objectMapper()
    private val conn = Database.connection

    init {
        Runtime
            .getRuntime()
            .addShutdownHook(Thread { conn.close() })
    }

    fun syncTable(link: String, clazz: KClass<Any>) {
        log.info("Download link=$link")
        val httpResponse = httpClient.send(
            HttpRequest.newBuilder(URI.create(link)).build(),
            HttpResponse.BodyHandlers.ofInputStream(),
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
                        (clazz as Any).javaClass
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