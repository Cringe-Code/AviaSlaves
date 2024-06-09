package app.aviaslaves.tickets

import app.aviaslaves.tickets.common.AuthPlugin
import app.aviaslaves.tickets.common.Environment
import app.aviaslaves.tickets.routes.ticketRoutes
import app.aviaslaves.tickets.schema.entities.Orders
import app.aviaslaves.tickets.schema.initDatabase
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

fun main() {
    initDatabase()

    embeddedServer(Netty, port = 3000) {
        install(ContentNegotiation) {
            json()
        }
        install(AuthPlugin)

        routing {
            ticketRoutes()
        }

        launchPeriodicStatusUpdate()

    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        Environment.kafkaClient.close()
    })
}

@OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
fun Application.launchPeriodicStatusUpdate() {
    GlobalScope.launch {
        periodicallyChangeOrderStatus()
    }
}

@OptIn(ExperimentalTime::class)
suspend fun periodicallyChangeOrderStatus() {
    while (true) {
        val delayDuration = Random.nextLong(1000, 5000) // Random delay between 1 and 5 seconds
        delay(delayDuration.milliseconds)

        newSuspendedTransaction {
            // Update all orders with status 1 to status 2
            Orders.update({ Orders.status eq 1 }) {
                it[status] = 2
            }
            // Update all orders with status 2 to status 3
            Orders.update({ Orders.status eq 2 }) {
                it[status] = 3
            }
        }
    }
}