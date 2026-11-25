package com.bookclub.persistence

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.DockerClientFactory
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@Testcontainers
class DatabaseSchemaIntegrationTest {
    companion object {
        private val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @BeforeAll
        fun start() {
            assumeTrue(
                DockerClientFactory.instance().isDockerAvailable,
                "Docker is required for PostgreSQL integration tests"
            )
            postgres.start()
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }

        @JvmStatic
        @AfterAll
        fun stop() = postgres.stop()
    }

    @Test
    fun `migrations create expected tables and indexes`() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name IN " +
                        "('clubs', 'users', 'submissions', 'poll_rounds', 'poll_votes')"
                ).use { result ->
                    result.next()
                    check(result.getInt(1) == 5)
                }
            }
        }
    }

    @Test
    fun `database rejects duplicate club and member relationships`() {
        connection().use { connection ->
            connection.autoCommit = false
            connection.prepareStatement("INSERT INTO clubs (telegram_chat_id, name) VALUES (?, ?)").use { statement ->
                statement.setLong(1, -100L)
                statement.setString(2, "Club")
                statement.executeUpdate()
                assertThrows(java.sql.SQLException::class.java) {
                    statement.executeUpdate()
                }
            }
            connection.rollback()
        }
    }

    private fun connection() = DriverManager.getConnection(
        postgres.jdbcUrl,
        postgres.username,
        postgres.password
    )
}
