package co.bdozer.libraries.clientcredentialsgrant

import co.bdozer.libraries.utils.Beans
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object AccessTokenProvider {
    private val httpClient = Beans.httpClient()
    private val objectMapper = Beans.objectMapper()
    
    /**
     * Perform a client credentials exchange to get JWT token
     */
    fun getAccessToken(): String {
        val httpResponse = httpClient.send(
            HttpRequest
                .newBuilder(URI.create("https://bdozer.us.auth0.com/oauth/token"))
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                    """
                    {
                        "client_id": "${System.getenv("CLIENT_ID") ?: error("env CLIENT_ID missing")}",
                        "client_secret": "${System.getenv("CLIENT_SECRET") ?: error("env CLIENT_SECRET missing")}",
                        "audience": "https://bdozer-api.herokuapp.com",
                        "grant_type": "client_credentials"
                    }
                    """.trimIndent()
                    )
                )
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        val response = objectMapper.readValue<ClientCredentialsGrantResponse>(httpResponse.body())
        return response.access_token
    }

}