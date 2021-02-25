package com.starburst.starburst.authn

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.starburst.starburst.ApiError
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthenticationFilter(
    private val jwtValidator: JwtValidator,
    private val objectMapper: ObjectMapper
): OncePerRequestFilter() {

    override fun doFilterInternal(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        chain: FilterChain
    ) {
        if (servletRequest.method == "OPTIONS" || !servletRequest.requestURI.startsWith("/api")) {
            doFilter(servletRequest, servletResponse, chain)
            return
        }

        val accessToken = extractJwtToken(servletRequest)
        try {
            jwtValidator.decodeAndVerify(accessToken)
            doFilter(servletRequest, servletResponse, chain)
        } catch (e: Exception) {
            error(servletResponse, e)
        }
    }

    fun error(resp: HttpServletResponse, e:Exception) {
        resp.addHeader("Access-Control-Allow-Origin", "*")
        resp.status = 401
        val apiError = ApiError(message = "cannot authenticate the request, underlying error ${e.message}")
        val body = objectMapper.writeValueAsString(apiError)
        resp.writer.println(body)
    }

    companion object {

        fun decodeJWT(req: HttpServletRequest): DecodedJWT {
            return JWT.decode(extractJwtToken(req)) ?: error("unable to decode JWT")
        }

        fun extractJwtToken(req: HttpServletRequest): String {
            val header = req.getHeader(HttpHeaders.AUTHORIZATION)
            try {
                return header.replaceFirst(("^Bearer ").toRegex(), "")
            } catch (e: Exception) {
                throw error(e)
            }
        }

    }

}
