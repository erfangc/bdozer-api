package com.bdozer.api.web.authn

import com.auth0.client.auth.AuthAPI
import com.auth0.exception.APIException
import com.bdozer.api.web.controlleradvice.ApiError
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthenticationFilter(
    private val jwtValidator: JwtValidator,
    private val objectMapper: ObjectMapper,
    private val userProvider: UserProvider,
) : OncePerRequestFilter() {

    private val authAPI = AuthAPI("https://bdozer.us.auth0.com", "...", "...")

    override fun doFilterInternal(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        chain: FilterChain
    ) {
        if (allowUnauthenticated(servletRequest)) {
            doFilter(servletRequest, servletResponse, chain)
        } else {
            try {
                val accessToken = getAccessToken(servletRequest)
                jwtValidator.decodeAndVerify(accessToken)
                setUser(accessToken)
                doFilter(servletRequest, servletResponse, chain)
            } catch (e: Exception) {
                error(servletResponse, e)
            }
        }
    }

    private fun setUser(accessToken: String) {
        try {
            val user = authAPI.userInfo(accessToken).execute()
            userProvider.set(user)
        } catch (ex: APIException) {
            // unable to retrieve user profile
        }
    }

    private fun allowUnauthenticated(servletRequest: HttpServletRequest) =
        servletRequest.method == "OPTIONS" || !servletRequest.requestURI.startsWith("/api")

    fun error(resp: HttpServletResponse, e: Exception) {
        resp.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        resp.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString())
        resp.status = 401
        val apiError = ApiError(message = e.message ?: "unable to verify JWT token")
        val body = objectMapper.writeValueAsString(apiError)
        resp.writer.println(body)
    }

    companion object {

        fun getAccessToken(req: HttpServletRequest): String {
            val header = req.getHeader(HttpHeaders.AUTHORIZATION) ?: error("please provide authentication credentials")
            return header.replaceFirst(("^Bearer ").toRegex(), "")
        }

    }

}
