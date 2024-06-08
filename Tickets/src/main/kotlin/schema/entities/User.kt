package app.aviaslaves.tickets.schema.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var email by Users.email
    //var originalId by Users.originalId
}

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    //val originalId = integer("originalId").uniqueIndex()
}