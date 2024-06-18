package app.aviaslaves.auth

import app.aviaslaves.auth.common.Environment
import app.aviaslaves.auth.routes.authRoutes
import app.aviaslaves.auth.routes.tokenRoutes
import app.aviaslaves.auth.schema.initDatabase
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*


fun main() {
    initDatabase()

    embeddedServer(Netty, port = 3000) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            authRoutes()
            tokenRoutes()
        }

    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        Environment.kafkaClient.close()
    })
}
