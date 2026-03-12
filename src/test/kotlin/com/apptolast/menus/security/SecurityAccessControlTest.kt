package com.apptolast.menus.security

import com.apptolast.menus.AbstractIntegrationTest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@DisplayName("Security — Access Control Tests")
class SecurityAccessControlTest : AbstractIntegrationTest() {

    private fun noErrorRestClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .defaultStatusHandler({ _ -> true }, { _, _ -> })
        .build()

    private fun registerAndGetToken(): String {
        val email = "security_test_${System.currentTimeMillis()}@example.com"
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(RegisterRequest(email, "SecurePass123!"))
            .retrieve()
            .toEntity(AuthResponse::class.java)
        return response.body?.accessToken ?: error("No token returned from register")
    }

    @Test
    @DisplayName("GET /api/v1/allergens is accessible without authentication")
    fun allergensPublicEndpointAccessible() {
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/allergens")
            .retrieve()
            .toEntity(List::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    @DisplayName("GET /api/v1/admin/restaurant returns 401 or 403 without authentication")
    fun adminEndpointRequiresAuth() {
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/admin/restaurant")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED)
    }

    @Test
    @DisplayName("GET /api/v1/admin/restaurant returns 403 for USER role (non-admin)")
    fun adminEndpointDeniedForUserRole() {
        val token = registerAndGetToken()
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/admin/restaurant")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @DisplayName("GET /api/v1/users/me/allergen-profile returns 401 without authentication")
    fun allergenProfileRequiresAuthentication() {
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/users/me/allergen-profile")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
    }

    @Test
    @DisplayName("GET /api/v1/users/me/allergen-profile returns 404 for authenticated user with no profile")
    fun allergenProfileReturns404WhenNoProfile() {
        val token = registerAndGetToken()
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/users/me/allergen-profile")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    @DisplayName("GET /api/v1/restaurants requires authentication in MVP")
    fun restaurantsRequiresAuth() {
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/restaurants")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
    }

    @Test
    @DisplayName("Swagger UI is accessible without authentication")
    fun swaggerUiAccessible() {
        val response = noErrorRestClient()
            .get()
            .uri("/swagger-ui/index.html")
            .retrieve()
            .toEntity(String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    @DisplayName("Actuator health is accessible without authentication")
    fun actuatorHealthAccessible() {
        val response = noErrorRestClient()
            .get()
            .uri("/actuator/health")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 or 403")
    fun unauthenticatedRequestReturnsUnauthorized() {
        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/users/me/allergen-profile")
            .retrieve()
            .toEntity(Map::class.java)
        assertThat(response.statusCode).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN)
    }
}
