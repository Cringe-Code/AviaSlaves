package app.aviaslaves.tickets

import app.aviaslaves.tickets.common.Environment
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main() {
    Environment.kafkaClient.sendMessage("1", "2")
    embeddedServer(Netty, port = 3001) {
        install(ContentNegotiation) {
            json()
        }
        routing {
        }

    }.start(wait = true)
}