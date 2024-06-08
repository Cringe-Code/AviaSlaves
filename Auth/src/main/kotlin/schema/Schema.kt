package app.aviaslaves.auth.schema

import app.aviaslaves.auth.schema.entities.Users
import app.aviaslaves.auth.schema.entities.Sessions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import app.aviaslaves.auth.common.Environment.databaseURL

fun initDatabase() {
    Database.connect(
        databaseURL,
        driver = "org.postgresql.Driver"
    )

    transaction {
        SchemaUtils.create(Users, Sessions)
    }
}