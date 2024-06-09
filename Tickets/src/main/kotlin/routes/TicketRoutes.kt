package app.aviaslaves.tickets.routes

import app.aviaslaves.tickets.controllers.buyTicket
import app.aviaslaves.tickets.controllers.getTicket
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.ticketRoutes() {
    route("/tickets") {
        post("/buy") {
            call.buyTicket()
        }
        post ("/get") {
            call.getTicket()
        }
    }
}