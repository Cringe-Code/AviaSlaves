package app.aviaslaves.auth.common

import app.aviaslaves.auth.kafka.Kafka
import io.github.cdimascio.dotenv.Dotenv
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import mu.KotlinLogging
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec

object Environment {
    private val dotenv: Dotenv = Dotenv.configure()
        .directory(".env")
        .ignoreIfMissing()
        .load()

    val bindPort: String by lazy { dotenv["BIND_PORT"] ?: "3000" }
    val databaseURL: String by lazy { "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME?user=$DB_USER&password=$DB_PASSWORD" }

    val accessPrivateKey: RSAPrivateKey = loadPrivateKey("jwt_key.pem")
    val refreshPrivateKey: RSAPrivateKey = loadPrivateKey("jwt_key2.pem")
    val accessPublicKey: RSAPublicKey = loadPublicKey("jwt_public_key.pem")
    val refreshPublicKey: RSAPublicKey = loadPublicKey("jwt_public_key2.pem")

    private val DB_HOST: String by lazy { dotenv["DATABASE_HOST"] ?: throw IllegalArgumentException("Database HOST is not defined") }
    private val DB_PORT: Int by lazy { dotenv["DATABASE_PORT"]?.toInt() ?: 5432 }
    private val DB_NAME: String by lazy { dotenv["DATABASE_NAME"] ?: throw IllegalArgumentException("Database name is not defined") }
    private val DB_USER: String by lazy { dotenv["DATABASE_USER"] ?: throw IllegalArgumentException("Database user is not defined") }
    private val DB_PASSWORD: String by lazy { dotenv["DATABASE_PASS"] ?: throw IllegalArgumentException("Database password is not defined") }

    private val KAFKA_URL: String by lazy { dotenv["KAFKA_URL"] ?: "kafka:9092" }

    private fun loadPrivateKey(filePath: String): RSAPrivateKey {
        val keyFile = File(filePath)
        val keyBytes = keyFile.readText(Charsets.UTF_8)
            .replace("\n", "")
            .replace("\r", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
        val decodedKey = Base64.getDecoder().decode(keyBytes)
        val keySpec = PKCS8EncodedKeySpec(decodedKey)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun loadPublicKey(filePath: String): RSAPublicKey {
        val keyFile = File(filePath)
        val keyBytes = keyFile.readText(Charsets.UTF_8)
            .replace("\n", "")
            .replace("\r", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
        val decodedKey = Base64.getDecoder().decode(keyBytes)
        val keySpec = X509EncodedKeySpec(decodedKey)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
    }

    @JvmField
    val logger = KotlinLogging.logger {}

    val usernameRegex = Regex("^[a-zA-Z0-9_]{3,30}\$")
    val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
    val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$")
    val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\$")

    val kafkaClient = Kafka.configure {
        onServer(KAFKA_URL)
        initializeTopics("user-profile-init", "user-profile-created")
        listen("user-profile-created")
        send("user-profile-init")
        onMessage { _, key, value ->
            logger.warn("UserID $key, E-Mail: $value: Profile is initialized!")
        }
    }
}
