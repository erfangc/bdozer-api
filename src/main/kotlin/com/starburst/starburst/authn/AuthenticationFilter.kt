package com.starburst.starburst.authn

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.starburst.starburst.controlleradvice.ApiError
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
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        chain: FilterChain
    ) {
        if (allowedByDefault(servletRequest)) {
            doFilter(servletRequest, servletResponse, chain)
            return
        }
        try {
            jwtValidator.decodeAndVerify(extractJwtToken(servletRequest))
            doFilter(servletRequest, servletResponse, chain)
        } catch (e: Exception) {
            error(servletResponse, e)
        }
    }

    private fun allowedByDefault(servletRequest: HttpServletRequest) =
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

        fun decodeJWT(req: HttpServletRequest): DecodedJWT {
            return JWT.decode(extractJwtToken(req)) ?: error("unable to decode JWT")
        }

        fun extractJwtToken(req: HttpServletRequest): String {
            val header = req.getHeader(HttpHeaders.AUTHORIZATION)
            return header.replaceFirst(("^Bearer ").toRegex(), "")
        }

    }

}
