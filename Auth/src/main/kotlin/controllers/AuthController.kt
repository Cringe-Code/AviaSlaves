package app.aviaslaves.auth.controllers

import app.aviaslaves.auth.common.*
import app.aviaslaves.auth.schema.entities.Users
import app.aviaslaves.auth.schema.entities.Sessions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

@Serializable
data class LoginData (
    val email: String,
    val password: String,
    val deviceIdentifier: String
)

@Serializable
data class SignupData (
    val username: String,
    val password: String,
    val email: String,
    val deviceIdentifier: String
)
suspend fun ApplicationCall.loginUser() {
    val body = receive<LoginData>()

    var userPasswordHash: String? = null
    var dbUserId: Int? = null
    try {
        transaction {
            val resultRow = Users
                .select( Users.password, Users.id )
                .where { Users.email eq body.email }
                .singleOrNull()

            if (resultRow == null) {
                throw IllegalArgumentException("Email does not exist: ${body.email}")
            }

            dbUserId = resultRow[Users.id].value
            userPasswordHash = resultRow[Users.password]
        }
    } catch (e: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, "Email does not exist: ${body.email}")
        return
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        return
    }

    if (!ArgonCryptor.verifyHash(body.password.toCharArray(), userPasswordHash!!)) {
        respond(HttpStatusCode.Unauthorized, "Incorrect password!")
        return
    }

    val accessToken = JwtUtils.getAccessToken(JwtPayload(body.email, body.deviceIdentifier))
    val refreshToken =  JwtUtils.getRefreshToken(JwtPayload(body.email, body.deviceIdentifier))
    val hashedRefreshToken = ArgonCryptor.hashPassword(refreshToken.toCharArray())

    insertOrUpdateSession(dbUserId!!, body.deviceIdentifier, hashedRefreshToken)

    setCookie("refreshToken", refreshToken, httpOnly = true, path = "/", maxAge = 60 * 60 * 24 * 30)
    setCookie("accessToken", accessToken, httpOnly = true, path = "/", maxAge = 60 * 60)

    respond(HttpStatusCode.OK, "Success!")
    return
}

suspend fun ApplicationCall.signupUser() {
    val body = receive<SignupData>()
    verifyData(body)

    val hashedPassword = ArgonCryptor.hashPassword(body.password.toCharArray())

    var newUserId: Int? = null

    try {
        transaction {
            val exists = Users
                .select(Users.username, Users.email)
                .where { (Users.username eq body.username) or (Users.email eq body.email) }
                .singleOrNull()

            if (exists != null) {
                throw IllegalArgumentException("Username or email already exists: $exists")
            }

            newUserId = (Users.insert {
                it[Users.username] = body.username
                it[Users.password] = hashedPassword
                it[Users.email] = body.email
            } get Users.id).value
        }
    } catch (e: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, "Username or email already exists!")
        return
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        return
    }

    val accessToken = JwtUtils.getAccessToken(JwtPayload(body.email, body.deviceIdentifier))
    val refreshToken =  JwtUtils.getRefreshToken(JwtPayload(body.email, body.deviceIdentifier))
    val hashedRefreshToken = ArgonCryptor.hashPassword(refreshToken.toCharArray())

    insertSession(newUserId!!, body.deviceIdentifier, hashedRefreshToken)

    setCookie("refreshToken", refreshToken, httpOnly = true, path = "/", maxAge = 60 * 60 * 24 * 30)
    setCookie("accessToken", accessToken, httpOnly = true, path = "/", maxAge = 60 * 60)

    Environment.kafkaClient.sendMessage(newUserId!!.toString(), body.email)

    respond(HttpStatusCode.OK, "Success!")
    return
}

private suspend fun ApplicationCall.insertSession(usId: Int, deviceIdentifier: String, hashedRefreshToken: String) {
    try {
        transaction {
            Sessions.insert {
                it[userId] = usId
                it[token] = hashedRefreshToken
                it[device] = deviceIdentifier
                it[expiresAt] = LocalDateTime.now().plusDays(30)
            }
        }
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        return
    }
}

private suspend fun ApplicationCall.insertOrUpdateSession(usId: Int, deviceIdentifier: String, hashedRefreshToken: String) {
    try {
        transaction {
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
                    it[userId] = usId
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

private suspend fun ApplicationCall.verifyData(data: SignupData) {
    if (!Environment.usernameRegex.matches(data.username)) {
        respond(HttpStatusCode.BadRequest, "Invalid username!")
        return
    }

    if (!Environment.passwordRegex.matches(data.password)) {
        respond(HttpStatusCode.BadRequest, "Invalid password!")
        return
    }

    if (!Environment.emailRegex.matches(data.email)) {
        respond(HttpStatusCode.BadRequest, "Invalid email!")
        return
    }

    if (!Environment.uuidRegex.matches(data.deviceIdentifier)) {
        respond(HttpStatusCode.BadRequest, "Invalid device identifier!")
        return
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
