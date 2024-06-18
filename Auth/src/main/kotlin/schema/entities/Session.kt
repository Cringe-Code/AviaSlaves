package app.aviaslaves.auth.schema.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

class Session(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Session>(Sessions)

    var token by Sessions.token
    var user by User referencedOn Sessions.userId
    var device by Sessions.device
    var createdAt by Sessions.createdAt
    var expiresAt by Sessions.expiresAt
}

object Sessions : IntIdTable() {
    val token = text("token").uniqueIndex()
    val userId = reference("userId", Users)
    val device = varchar("device", 255).uniqueIndex()
    val createdAt = datetime("createdAt").clientDefault { LocalDateTime.now() }
    val expiresAt = datetime("expiresAt")
}