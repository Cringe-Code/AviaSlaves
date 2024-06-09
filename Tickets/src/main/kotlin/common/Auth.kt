package app.aviaslaves.tickets.common

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AuthConfig

val AuthPlugin = createApplicationPlugin("AuthPlugin", ::AuthConfig) {
    onCall { call ->
        Environment.logger.warn("plugin")
        val status = call.verifyAccessToken()
        if (status != HttpStatusCode.OK) {
            call.respond(status, "Unauthorized")
            return@onCall
        }
    }
}

// Function to verify access token by making an HTTP request
suspend fun ApplicationCall.verifyAccessToken(): HttpStatusCode {
    val cookiesHeader = request.headers["Cookie"] ?: return HttpStatusCode.Unauthorized

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://traefik/token/verify"))
        .header("Cookie", cookiesHeader)
        .GET()
        .build()

    val response = withContext(Dispatchers.IO) {
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    return if (response.statusCode() == HttpStatusCode.OK.value) {
        HttpStatusCode.OK
    } else {
        HttpStatusCode.Unauthorized
    }
}