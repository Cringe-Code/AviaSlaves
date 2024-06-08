package app.aviaslaves.tickets

import app.aviaslaves.tickets.common.Environment
import app.aviaslaves.tickets.schema.initDatabase
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main() {
    initDatabase()

    embeddedServer(Netty, port = 3000) {
        install(ContentNegotiation) {
            json()
        }
        routing {
        }

    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        Environment.kafkaClient.close()
    })
}