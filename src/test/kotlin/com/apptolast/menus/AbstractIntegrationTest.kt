package com.apptolast.menus

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer

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
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("menusdb_test")
            .withUsername("menus_test")
            .withPassword("menus_test")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("app.jwt.secret") { "test-secret-key-minimum-64-bytes-long-for-HS512-algorithm-testing!!" }
            registry.add("app.cors.allowed-origins") { "http://localhost:3000" }
            registry.add("app.google.client-id") { "test-google-client-id" }
            registry.add("spring.security.oauth2.client.registration.google.client-id") { "test-google-client-id" }
            registry.add("spring.security.oauth2.client.registration.google.client-secret") { "test-google-client-secret" }
        }
    }
}
