package com.apptolast.menus.dish

import com.apptolast.menus.AbstractIntegrationTest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.model.entity.AdminWhitelist
import com.apptolast.menus.auth.repository.AdminWhitelistRepository
import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.util.UUID

@DisplayName("Dish Price from Recipe — Integration Tests")
class DishPriceFromRecipeIntegrationTest : AbstractIntegrationTest() {

    @Autowired private lateinit var restaurantRepository: RestaurantRepository
    @Autowired private lateinit var menuRepository: MenuRepository
    @Autowired private lateinit var menuSectionRepository: MenuSectionRepository
    @Autowired private lateinit var adminWhitelistRepository: AdminWhitelistRepository

    private lateinit var token: String
    private lateinit var restaurantId: UUID
    private lateinit var sectionId: UUID

    private fun noErrorRestClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .defaultStatusHandler({ _ -> true }, { _, _ -> })
        .build()

    private fun registerAdmin(): String {
        val email = "dish_admin_${System.currentTimeMillis()}@example.com"
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
            Restaurant(name = "Dish Price Restaurant ${System.currentTimeMillis()}", slug = "dp-${System.currentTimeMillis()}")
        )
        restaurantId = restaurant.id
        val menu = menuRepository.save(
            Menu(restaurantId = restaurant.id, name = "Test Menu")
        )
        val section = menuSectionRepository.save(
            MenuSection(menu = menu, name = "Test Section")
        )
        sectionId = section.id
    }

    @Test
    @DisplayName("Dish without recipe has null price")
    fun dishWithoutRecipeHasNullPrice() {
        val dishRequest = DishRequest(
            name = "No Recipe Dish",
            sectionId = sectionId
        )
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(dishRequest)
            .retrieve()
            .toEntity(DishResponse::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.price).isNull()
    }

    @Test
    @DisplayName("Public dish endpoint returns dishes")
    fun publicDishEndpointReturnsDishes() {
        val dishRequest = DishRequest(
            name = "Public Dish",
            sectionId = sectionId
        )
        noErrorRestClient()
            .post()
            .uri("/api/v1/admin/dishes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(dishRequest)
            .retrieve()
            .toBodilessEntity()

        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/restaurants/$restaurantId/sections/$sectionId/dishes")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<List<DishResponse>>() {})

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotEmpty
    }
}
