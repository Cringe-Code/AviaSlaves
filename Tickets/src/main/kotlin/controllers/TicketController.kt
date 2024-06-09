package app.aviaslaves.tickets.controllers

import app.aviaslaves.tickets.common.Environment
import app.aviaslaves.tickets.schema.entities.Orders
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class BuyData (
    val userId: Int,
    val departure: Int,
    val destination: Int
)

@Serializable
data class GetData (
    val orderId: Int
)

@Serializable
data class TicketResponse(
    val orderId: Int,
    val status: Int?,
    val fromStationId: Int?,
    val toStationId: Int?,
    val created: String?,
    val userId: Int?
)

suspend fun ApplicationCall.buyTicket() {
    val body = receive<BuyData>()

    try {
        transaction {
            Orders.insert{
                it[userId] = body.userId
                it[fromStationId] = body.departure
                it[toStationId] = body.destination
                it[status] = 1
            }
        }
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
        return
    }

    respond(HttpStatusCode.OK, "Success!")
}

suspend fun ApplicationCall.getTicket() {
    val body = receive<GetData>()

    try {
        val orderDetails = transaction {
            Orders.selectAll()
                .where { (Orders.id eq body.orderId) }
                .singleOrNull()
        }

        if (orderDetails == null) {
            respond(HttpStatusCode.NotFound, "Order not found")
            return
        }

        val orderStatus = orderDetails[Orders.status]
        val orderDeparture = orderDetails[Orders.fromStationId]
        val orderDestination = orderDetails[Orders.toStationId]
        val orderCreated = orderDetails[Orders.created]
        val orderUser = orderDetails[Orders.userId]

        val response = TicketResponse(
            orderId = body.orderId,
            status = orderStatus,
            fromStationId = orderDeparture.value,
            toStationId = orderDestination.value,
            created = orderCreated.toString(),
            userId = orderUser.value
        )

        respond(HttpStatusCode.OK, response)
    } catch (e: Exception) {
        Environment.logger.error { e.message }
        respond(HttpStatusCode.InternalServerError, "Unexpected server error, possibly database issue.")
    }
}