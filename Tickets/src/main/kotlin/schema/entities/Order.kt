package app.aviaslaves.tickets.schema.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

class Order(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Order>(Orders)

    var user by User referencedOn Orders.userId
    var fromStation by Station referencedOn Orders.fromStationId
    var toStation by Station referencedOn Orders.toStationId
    var status by Orders.status
    var created by Orders.created
}

object Orders : IntIdTable() {
    val userId = reference("user_id", Users)
    val fromStationId = reference("from_station_id", Stations)
    val toStationId = reference("to_station_id", Stations)
    val status = integer("status")
    val created = datetime("created").clientDefault { LocalDateTime.now() }
}