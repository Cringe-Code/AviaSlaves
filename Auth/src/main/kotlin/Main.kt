package app.aviaslaves.auth

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Users : IntIdTable() {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val isUserProfileInitialised = bool("isUserProfileInitialised").default(false)
    val isGameProfileInitialised = bool("isGameProfileInitialised").default(false)
}

object RefreshTokens : IntIdTable() {
    val token = varchar("token", 255).uniqueIndex()
    val userId = reference("userId", Users)
    val device = varchar("device", 255).uniqueIndex()
    val createdAt = datetime("createdAt").clientDefault { LocalDateTime.now() }
    val expiresAt = datetime("expiresAt")
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var password by Users.password
    var isUserProfileInitialised by Users.isUserProfileInitialised
    var isGameProfileInitialised by Users.isGameProfileInitialised
    val refreshTokens by RefreshToken referrersOn RefreshTokens.userId
}

class RefreshToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RefreshToken>(RefreshTokens)

    var token by RefreshTokens.token
    var user by User referencedOn RefreshTokens.userId
    var device by RefreshTokens.device
    var createdAt by RefreshTokens.createdAt
    var expiresAt by RefreshTokens.expiresAt
}

fun main() {
    Database.connect(
        url = System.getenv("DATABASE_URL"),
        driver = "org.postgresql.Driver"
    )

    transaction {
        SchemaUtils.create(Users, RefreshTokens)
    }
}
