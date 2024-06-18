package app.aviaslaves.auth.schema.entities

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var email by Users.email
    var password by Users.password
    var created by Users.created
    val sessions by Session referrersOn Sessions.userId
}

object Users : IntIdTable() {
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val created = datetime("created").clientDefault { LocalDateTime.now() }
}