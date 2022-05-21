package co.bdozer.jobs

import co.bdozer.libraries.clientcredentialsgrant.AccessTokenFetcher
import co.bdozer.libraries.utils.Beans
import com.bdozer.api.models.dataclasses.BuildZacksModelResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Main")
private val accessToken = AccessTokenFetcher.getAccessToken()
private val httpClient = Beans.httpClient()
private val objectMapper = Beans.objectMapper()
private val apiEndpoint = System.getenv("API_ENDPOINT") ?: "http://localhost:8080"

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
                    buildZacksModelResponse.id,
                    buildZacksModelResponse.ticker,
                    buildZacksModelResponse.targetPrice,
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
        } catch (e: Exception) {
            log.error("Unable to complete HTTP request for ticker={}", ticker, e)
            failed++
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