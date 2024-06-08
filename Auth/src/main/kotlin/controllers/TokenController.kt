package app.aviaslaves.auth.controllers

import app.aviaslaves.auth.common.ArgonCryptor
import app.aviaslaves.auth.common.Environment
import app.aviaslaves.auth.common.JwtUtils
import app.aviaslaves.auth.schema.entities.Sessions
import app.aviaslaves.auth.schema.entities.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

suspend fun ApplicationCall.verifyAccessToken() {
    val accessToken = request.cookies["accessToken"]

    return if (accessToken != null) {
        val payload = JwtUtils.verifyAccessToken(accessToken)
        if (payload) {
            respond(HttpStatusCode.OK, "Success!")
        } else {
            respond(HttpStatusCode.Unauthorized, "Invalid or expired access token")
        }
    } else {
        respond(HttpStatusCode.Unauthorized, "Access token not found")
    }
}
suspend fun ApplicationCall.verifyRefreshTokenAndGetData() {
    val refreshToken = request.cookies["refreshToken"]

    return if (refreshToken != null) {
        val payload = JwtUtils.verifyRefreshToken(refreshToken)
        if (payload != null && isRefreshTokenValid(payload.email, payload.deviceIdentifier, refreshToken)) {
            respond(HttpStatusCode.OK, payload)
        } else {
            respond(HttpStatusCode.Unauthorized, "Invalid or expired refresh token")
        }

    } else {
        respond(HttpStatusCode.Unauthorized, "Refresh token not found")
    }
}

suspend fun ApplicationCall.refreshTokens() {
    val refreshToken = request.cookies["refreshToken"]

    if (refreshToken != null) {
        val payload = JwtUtils.verifyRefreshToken(refreshToken)
        if (payload != null && isRefreshTokenValid(payload.email, payload.deviceIdentifier, refreshToken)) {
            // Generate new tokens
            val newAccessToken = JwtUtils.getAccessToken(payload)
            val newRefreshToken = JwtUtils.getRefreshToken(payload)

            val newHashedRefreshToken = ArgonCryptor.hashPassword(newRefreshToken.toCharArray())

            insertOrUpdateSession(payload.email, payload.deviceIdentifier, newHashedRefreshToken)

            // Set new tokens in cookies
            setCookie("accessToken", newAccessToken, httpOnly = true, path = "/", maxAge = 3600) // 1 hour expiry
            setCookie("refreshToken", newRefreshToken, httpOnly = true, path = "/", maxAge = 604800) // 1 week expiry

            respond(HttpStatusCode.OK, "Success!")
        } else {
            respond(HttpStatusCode.Unauthorized, "Invalid or expired refresh token")
        }
    } else {
        respond(HttpStatusCode.Unauthorized, "Refresh token not found")
    }
}

private fun ApplicationCall.setCookie(name: String, value: String, httpOnly: Boolean, path: String, maxAge: Int) {
    response.cookies.append(
        Cookie (
            name = name,
            value = value,
            maxAge = maxAge,
            path = path,
            httpOnly = httpOnly,
            secure = false
        )
    )
}

private suspend fun ApplicationCall.insertOrUpdateSession(usEmail: String, deviceIdentifier: String, hashedRefreshToken: String) {
    try {
        transaction {
            val usId = Users
                .select( Users.id )
                .where { Users.email eq usEmail }
                .singleOrNull()?.get(Users.id)?.value

            // Check if the device already exists
            val existingSession = Sessions.selectAll()
                .where { (Sessions.userId eq usId) and (Sessions.device eq deviceIdentifier) }
                .singleOrNull()

            if (existingSession != null) {
                // Update the existing session
                Sessions.update({ Sessions.id eq existingSession[Sessions.id] }) {
                    it[token] = hashedRefreshToken
                    it[expiresAt] = LocalDateTime.now().plusDays(30)
                }
            } else {
                // Insert a new session
                Sessions.insert {
                    it[userId] = usId!!
                    it[token] = hashedRefreshToken
                    it[device] = deviceIdentifier
                    it[expiresAt] = LocalDateTime.now().plusDays(30)
                }
            }
        }
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        return
    }
}


suspend fun ApplicationCall.isRefreshTokenValid(email: String, deviceId: String, refreshToken: String): Boolean {
    return try {
        val hashedRefreshToken = transaction {
            val userId = Users
                .select(Users.id)
                .where { Users.email eq email }
                .singleOrNull()?.get(Users.id)?.value

            if (userId != null) {
                Sessions
                    .select (Sessions.token)
                    .where { (Sessions.userId eq userId) and (Sessions.device eq deviceId)}
                    .singleOrNull()?.get(Sessions.token)
            } else {
                false
            }
        }

        if (hashedRefreshToken != null) {
            ArgonCryptor.verifyHash(refreshToken.toCharArray(), hashedRefreshToken.toString())
        } else {
            false
        }
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        false
    }
}