package com.apptolast.menus.recipe

import com.apptolast.menus.AbstractIntegrationTest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.model.entity.AdminWhitelist
import com.apptolast.menus.auth.repository.AdminWhitelistRepository
import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.UpdateRecipeRequest
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.util.UUID

@DisplayName("Recipe Price — Integration Tests")
class RecipePriceIntegrationTest : AbstractIntegrationTest() {

    @Autowired private lateinit var restaurantRepository: RestaurantRepository
    @Autowired private lateinit var adminWhitelistRepository: AdminWhitelistRepository

    private lateinit var token: String
    private lateinit var restaurantId: UUID

    private fun noErrorRestClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .defaultStatusHandler({ _ -> true }, { _, _ -> })
        .build()

    private fun registerAdmin(): String {
        val email = "recipe_admin_${System.currentTimeMillis()}@example.com"
        adminWhitelistRepository.save(AdminWhitelist(email = email))
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/auth/register-admin")
            .contentType(MediaType.APPLICATION_JSON)
            .body(RegisterAdminRequest(email, "SecurePass123!"))
            .retrieve()
            .toEntity(AuthResponse::class.java)
        return response.body?.accessToken ?: error("No token")
    }

    @BeforeEach
    fun setup() {
        token = registerAdmin()
        val restaurant = restaurantRepository.save(
            Restaurant(name = "Test Restaurant ${System.currentTimeMillis()}", slug = "test-${System.currentTimeMillis()}")
        )
        restaurantId = restaurant.id
    }

    @Test
    @DisplayName("POST create recipe with price returns price in response")
    fun createRecipeWithPrice() {
        val request = CreateRecipeRequest(
            restaurantId = restaurantId,
            name = "Paella Test",
            price = BigDecimal("18.50")
        )
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/restaurants/$restaurantId/recipes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toEntity(RecipeResponse::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.price).isEqualByComparingTo(BigDecimal("18.50"))
    }

    @Test
    @DisplayName("POST create recipe without price returns null price")
    fun createRecipeWithoutPrice() {
        val request = CreateRecipeRequest(
            restaurantId = restaurantId,
            name = "Gazpacho Test"
        )
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/restaurants/$restaurantId/recipes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toEntity(RecipeResponse::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.price).isNull()
    }

    @Test
    @DisplayName("PUT update recipe price changes the price")
    fun updateRecipePrice() {
        val createRequest = CreateRecipeRequest(
            restaurantId = restaurantId,
            name = "Tortilla Test",
            price = BigDecimal("10.00")
        )
        val created = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/restaurants/$restaurantId/recipes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(createRequest)
            .retrieve()
            .toEntity(RecipeResponse::class.java)
        val recipeId = created.body!!.id

        val updateRequest = UpdateRecipeRequest(price = BigDecimal("12.50"))
        val updated = noErrorRestClient()
            .put()
            .uri("/api/v1/admin/recipes/$recipeId")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(updateRequest)
            .retrieve()
            .toEntity(RecipeResponse::class.java)

        assertThat(updated.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(updated.body?.price).isEqualByComparingTo(BigDecimal("12.50"))
    }
}
