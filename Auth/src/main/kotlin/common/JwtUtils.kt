package app.aviaslaves.auth.common

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Date

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
}