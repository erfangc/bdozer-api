package co.bdozer.jobs

import co.bdozer.libraries.clientcredentialsgrant.AccessTokenFetcher
import co.bdozer.libraries.utils.Beans
import co.bdozer.libraries.utils.Database
import com.bdozer.api.models.dataclasses.BuildZacksModelResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.sql.Timestamp
import java.sql.Types
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")
private val accessToken = AccessTokenFetcher.getAccessToken()
private val httpClient = Beans.httpClient()
private val conn = Database.connection
private val objectMapper = Beans.objectMapper()
private val apiEndpoint = System.getenv("API_ENDPOINT") ?: "http://localhost:8080"

fun updateStatus(
    ticker: String, 
    result: BuildZacksModelResponse? = null,
    e: Exception? = null,
) {

    if (result == null && e == null) {
        error("result and e cannot be null")
    }
    val stmt = conn.prepareStatement(
        """
        insert into zacks_model_run_results (ticker, timestamp, status, stock_analysis_id, message) 
        values (?, ?, ?, ?, ?)
        on conflict on constraint zacks_model_run_results_pkey 
        do update set timestamp = ?, status = ?, stock_analysis_id = ?, message = ?
        """.trimIndent()
    )
    stmt.setString(1, ticker)
    
    if (result?.timestamp != null) {
        stmt.setTimestamp(2, Timestamp.from(result.timestamp))   
        stmt.setTimestamp(2 + 4, Timestamp.from(result.timestamp))   
    } else {
        stmt.setNull(2, Types.TIMESTAMP)
        stmt.setNull(2 + 4, Types.TIMESTAMP)
    }
    
    if (result?.status != null) {
        stmt.setInt(3, result.status)
        stmt.setInt(3 + 4, result.status)
    } else {
        stmt.setNull(3, Types.INTEGER)
        stmt.setNull(3 + 4, Types.INTEGER)
    }
    
    if (result?.id != null) {
        stmt.setString(4, result.id)
        stmt.setString(4 + 4, result.id)
    } else {
        stmt.setNull(4, Types.VARCHAR)
        stmt.setNull(4 + 4, Types.VARCHAR)
    }
    
    if (result?.message != null) {
        stmt.setString(5, result.message)
        stmt.setString(5 + 4, result.message)
    } else {
        stmt.setString(5, e?.message)
        stmt.setString(5 + 4, e?.message)
    }
    
    stmt.execute()
    stmt.close()
}

fun main() {

    val filename = "bdozer-api-batch-jobs/russell-1000-constituents.txt"
    log.info("Starting computing Zacks model on Russell 1000 constituent 10-Ks using embedded file $filename")
    val tickers = FileInputStream(filename)
        .bufferedReader()
        .readLines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    log.info("Found {} lines in {}", tickers.size, filename)

    var remaining = tickers.size
    var processed = 0
    var succeeded = 0
    var failed = 0

    fun run(ticker: String) {
        try {
            val httpResponse = httpClient.send(
                HttpRequest
                    .newBuilder(URI.create("$apiEndpoint/api/zacks-model-builder/$ticker"))
                    .PUT(BodyPublishers.noBody())
                    .header("Authorization", "Bearer $accessToken")
                    .build(),
                BodyHandlers.ofString(),
            )
            val buildZacksModelResponse = objectMapper.readValue<BuildZacksModelResponse>(httpResponse.body())
            if (httpResponse.statusCode() == 200) {
                log.info(
                    "Built Zacks model for ticker={} targetPrice={} id={}",
                    buildZacksModelResponse.ticker,
                    buildZacksModelResponse.targetPrice,
                    buildZacksModelResponse.id,
                )
                succeeded++
            } else {
                // try to deserialize the message
                log.error(
                    "Unable to build Zacks model for id={} ticker={} status={} message='{}'",
                    buildZacksModelResponse.id,
                    buildZacksModelResponse.ticker,
                    httpResponse.statusCode(),
                    buildZacksModelResponse.message
                )
                failed++
            }
            updateStatus(ticker = ticker, result = buildZacksModelResponse)
        } catch (e: Exception) {
            log.error("Unable to complete HTTP request for ticker={}", ticker, e)
            failed++
            updateStatus(ticker = ticker, e = e)
        } finally {
            remaining--
            processed++
            log.info(
                "Processing status remaining={} processed={} succeeded={} failed={}",
                remaining, processed, succeeded, failed
            )
        }
    }

    for (ticker in tickers) {
        run(ticker)
    }
    
    log.info("Finished processing")
    exitProcess(0)
}