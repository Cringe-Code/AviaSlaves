package app.aviaslaves.tickets.schema.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class Station(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Station>(Stations)

    var station by Stations.station
}

object Stations : IntIdTable() {
    val station = varchar("station", 50)
}