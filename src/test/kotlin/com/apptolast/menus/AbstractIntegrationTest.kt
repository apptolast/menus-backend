package com.apptolast.menus

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    protected fun baseUrl(): String = "http://localhost:$port"

    protected fun restClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .build()

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("menusdb_test")
            .withUsername("menus_test")
            .withPassword("menus_test")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.getJdbcUrl() }
            registry.add("spring.datasource.username") { postgres.getUsername() }
            registry.add("spring.datasource.password") { postgres.getPassword() }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("app.jwt.secret") { "test-secret-key-minimum-64-bytes-long-for-HS512-algorithm-testing!!" }
            registry.add("app.encryption.key") { "test-aes-256-key-exactly-32-byte" }
            registry.add("app.cors.allowed-origins") { "http://localhost:3000" }
            registry.add("spring.data.redis.repositories.enabled") { "false" }
            registry.add("management.health.redis.enabled") { "false" }
            registry.add("spring.autoconfigure.exclude") { "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration" }
        }
    }
}
