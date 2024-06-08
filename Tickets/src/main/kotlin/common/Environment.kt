package app.aviaslaves.tickets.common

import app.aviaslaves.tickets.kafka.Kafka
import app.aviaslaves.tickets.schema.entities.Users
import io.github.cdimascio.dotenv.Dotenv
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object Environment {
    private val dotenv: Dotenv = Dotenv.configure()
        .directory(".env")
        .ignoreIfMissing()
        .load()

    @JvmField
    val logger = KotlinLogging.logger {}


    val bindPort: String by lazy { dotenv["BIND_PORT"] ?: "3000" }
    private val KAFKA_URL: String by lazy { dotenv["KAFKA_URL"] ?: "kafka:9092" }
    val databaseURL: String by lazy { "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME?user=$DB_USER&password=$DB_PASSWORD" }


    private val DB_HOST: String by lazy { dotenv["DATABASE_HOST"] ?: throw IllegalArgumentException("Database HOST is not defined") }
    private val DB_PORT: Int by lazy { dotenv["DATABASE_PORT"]?.toInt() ?: 5432 }
    private val DB_NAME: String by lazy { dotenv["DATABASE_NAME"] ?: throw IllegalArgumentException("Database name is not defined") }
    private val DB_USER: String by lazy { dotenv["DATABASE_USER"] ?: throw IllegalArgumentException("Database user is not defined") }
    private val DB_PASSWORD: String by lazy { dotenv["DATABASE_PASS"] ?: throw IllegalArgumentException("Database password is not defined") }


    val kafkaClient = Kafka.configure {
        onServer(KAFKA_URL)
        initializeTopics("user-profile-init", "user-profile-created")
        listen("user-profile-init")
        send("user-profile-created")
        onMessage { client, key, value ->
            logger.warn("User profile initialization requested for UserID $key, E-Mail: $value")

            transaction {
                Users.insert {
                    it[Users.id] = key.toInt()
                    it[Users.email] = value
                }
            }

            client.sendMessage(key, value)
        }
    }
}