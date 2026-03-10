package com.apptolast.menus.auth

import com.apptolast.menus.AbstractIntegrationTest
import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Disabled("Requires Docker for Testcontainers PostgreSQL — run with Docker Desktop active")
@DisplayName("Auth API — Integration Tests")
class AuthIntegrationTest : AbstractIntegrationTest() {

    private fun noErrorRestClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .defaultStatusHandler({ _ -> true }, { _, _ -> })
        .build()

    @Test
    @DisplayName("POST /api/v1/auth/register returns 201 with tokens")
    fun registerReturns201() {
        val request = RegisterRequest(
            email = "integration_test_${System.currentTimeMillis()}@example.com",
            password = "SecurePass123!"
        )
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(AuthResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.accessToken).isNotBlank()
        assertThat(response.body?.refreshToken).isNotBlank()
        assertThat(response.body?.tokenType).isEqualTo("Bearer")
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 409 for duplicate email")
    fun registerDuplicateEmailReturns409() {
        val email = "duplicate_test_${System.currentTimeMillis()}@example.com"
        val request = RegisterRequest(email = email, password = "SecurePass123!")
        noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity()
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with tokens after register")
    fun loginReturnsTokens() {
        val email = "login_test_${System.currentTimeMillis()}@example.com"
        val password = "SecurePass123!"
        noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(RegisterRequest(email, password))
            .retrieve()
            .toBodilessEntity()
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(LoginRequest(email, password))
            .retrieve()
            .toEntity(AuthResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.accessToken).isNotBlank()
    }
}
