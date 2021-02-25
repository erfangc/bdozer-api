package com.starburst.starburst.authn

import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthenticationFilter(private val jwtValidator: JwtValidator): OncePerRequestFilter() {

    override fun doFilterInternal(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, chain: FilterChain) {
        if (servletRequest.method == "OPTIONS") {
            doFilter(servletRequest, servletResponse, chain)
            return
        }

        val authorization = servletRequest.getHeader(HttpHeaders.AUTHORIZATION)
        val accessToken = extractAccessToken(authorization)
        try {
            jwtValidator.decodeAndVerify(accessToken)
            doFilter(servletRequest, servletResponse, chain)
        } catch (e: Exception) {
            error(e)
        }
    }

    private fun extractAccessToken(authorization: String): String {
        try {
            return authorization.replaceFirst(("^Bearer ").toRegex(), "")
        } catch (e: Exception) {
            throw error(e)
        }
    }
}
