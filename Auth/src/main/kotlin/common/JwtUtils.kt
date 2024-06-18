package app.aviaslaves.auth.common

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import java.util.Date
import java.util.Base64


object JwtUtils {
    suspend fun getAccessToken(user: JwtPayload): String {
        return JWT.create()
            .withPayload(Json.encodeToString(user))
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour
            .sign(Algorithm.RSA256(null, Environment.accessPrivateKey))
    }

    suspend fun getRefreshToken(user: JwtPayload): String {
        return JWT.create()
            .withPayload(Json.encodeToString(user))
            .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)) // 30 days
            .sign(Algorithm.RSA256(null, Environment.refreshPrivateKey))
    }

    private val accessTokenVerifier: JWTVerifier by lazy {
        JWT.require(Algorithm.RSA256(Environment.accessPublicKey, null)).build()
    }

    private val refreshTokenVerifier: JWTVerifier by lazy {
        JWT.require(Algorithm.RSA256(Environment.refreshPublicKey, null)).build()
    }

    suspend fun verifyAccessToken(token: String): Boolean {
        return try {
            val decodedJWT = accessTokenVerifier.verify(token)
            true
        } catch (e: JWTVerificationException) {
            false
        }
    }

    suspend fun verifyRefreshToken(token: String): JwtPayload? {
        return try {
            val decodedJWT = refreshTokenVerifier.verify(token)
            extractPayload(decodedJWT)
        } catch (e: JWTVerificationException) {
            null
        }
    }

    private fun extractPayload(decodedJWT: DecodedJWT): JwtPayload {
        val payloadJson = String(Base64.getUrlDecoder().decode(decodedJWT.payload))
        val json = Json {
            ignoreUnknownKeys = true
        }
        val data = json.decodeFromString<JwtPayload>(payloadJson)
        return data
    }
}