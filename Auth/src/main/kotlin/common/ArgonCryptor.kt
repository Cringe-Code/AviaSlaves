package app.aviaslaves.auth.common

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types

/**
 * Argon2id password hashing utility.
 */
object ArgonCryptor {
    private val argon2: Argon2 = Argon2Factory.create(Argon2Types.ARGON2id)

    // Argon2id params
    private const val MEMORY_SIZE: Int = 10
    private const val ITERATIONS: Int = 65536
    private const val PARALLELISM: Int = 3

    /**
     * Hashes a password using Argon2id.
     *
     * @param password The password to hash.
     * @return The hashed password.
     */
    fun hashPassword(password: CharArray): String {
        return argon2.hash(MEMORY_SIZE, ITERATIONS, PARALLELISM, password)
            .also { argon2.wipeArray(password) }
    }

    /**
     * Verifies a password against a hash.
     *
     * @param password The password to verify.
     * @param hash The hash to verify against.
     * @return Whether the password matches the hash.
     */
    fun verifyHash(password: CharArray, hash: String): Boolean {
        return try {
            argon2.verify(hash, password)
                .also { argon2.wipeArray(password) }
        } catch (e: IllegalArgumentException) {
            Environment.logger.error("Hash verification failed: ${e.message}")
            false
        } catch (e: Exception) {
            Environment.logger.error("Unexpected error in hash verification: ${e.message}")
            false
        }
    }
}
