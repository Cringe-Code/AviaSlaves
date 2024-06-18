package app.aviaslaves.auth.common

import kotlinx.serialization.Serializable

@Serializable
data class JwtPayload(
    val email: String,
    val deviceIdentifier: String
)
