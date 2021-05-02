package com.bdozer.authn

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.springframework.stereotype.Service
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

@Service
class JwtValidator {

    private val issuer = "https://ease-wealth.us.auth0.com/"

    private val jwkProvider: JwkProvider = JwkProviderBuilder(issuer)
        .cached(5, 1, TimeUnit.HOURS)
        .build()

    fun decodeAndVerify(token: String): DecodedJWT {
        val decodedJWT = JWT.decode(token)
        //
        // find the kid from jwkProvider, use that to construct the public key needed
        //
        val kid = decodedJWT.keyId
        try {
            val jwtVerifier = jwtVerifierForKid(kid)
            return jwtVerifier.verify(decodedJWT)
        } catch (e: JwkException) {
            throw error(e)
        } catch (e: JWTVerificationException) {
            throw error(e)
        }

    }

    private fun jwtVerifierForKid(kid: String): JWTVerifier {
        val jwk = jwkProvider.get(kid)
        val publicKey = jwk.publicKey as RSAPublicKey
        return JWT.require(Algorithm.RSA256(publicKey, null)).withIssuer(issuer).acceptLeeway(0).build()
    }

}
