package app.aviaslaves.tickets.schema

import app.aviaslaves.tickets.schema.entities.Orders
import app.aviaslaves.tickets.schema.entities.Stations
import app.aviaslaves.tickets.schema.entities.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import app.aviaslaves.tickets.common.Environment.databaseURL

fun initDatabase() {
    Database.connect(
        databaseURL,
        driver = "org.postgresql.Driver"
    )

    transaction {
        SchemaUtils.create(Users, Stations, Orders)
    }
}