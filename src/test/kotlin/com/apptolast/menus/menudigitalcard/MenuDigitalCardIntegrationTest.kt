package com.apptolast.menus.menudigitalcard

import com.apptolast.menus.AbstractIntegrationTest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.model.entity.AdminWhitelist
import com.apptolast.menus.auth.repository.AdminWhitelistRepository
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.menudigitalcard.dto.request.CreateMenuDigitalCardRequest
import com.apptolast.menus.menudigitalcard.dto.response.MenuDigitalCardResponse
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

@DisplayName("Menu Digital Card — Integration Tests")
class MenuDigitalCardIntegrationTest : AbstractIntegrationTest() {

    @Autowired private lateinit var restaurantRepository: RestaurantRepository
    @Autowired private lateinit var menuRepository: MenuRepository
    @Autowired private lateinit var menuSectionRepository: MenuSectionRepository
    @Autowired private lateinit var dishRepository: DishRepository
    @Autowired private lateinit var adminWhitelistRepository: AdminWhitelistRepository

    private lateinit var token: String
    private lateinit var menuId: UUID
    private lateinit var dishId: UUID

    private fun noErrorRestClient(): RestClient = RestClient.builder()
        .baseUrl(baseUrl())
        .defaultStatusHandler({ _ -> true }, { _, _ -> })
        .build()

    private fun registerAdmin(): String {
        val email = "mdc_admin_${System.currentTimeMillis()}@example.com"
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
            Restaurant(name = "MDC Restaurant ${System.currentTimeMillis()}", slug = "mdc-${System.currentTimeMillis()}")
        )
        val menu = menuRepository.save(
            Menu(restaurantId = restaurant.id, name = "Test Menu")
        )
        menuId = menu.id
        val section = menuSectionRepository.save(
            MenuSection(menu = menu, name = "Test Section")
        )
        val dish = dishRepository.save(
            Dish(section = section, name = "Test Dish")
        )
        dishId = dish.id
    }

    @Test
    @DisplayName("POST create menu-digital-card assignment returns 201")
    fun createAssignment() {
        val request = CreateMenuDigitalCardRequest(menuId = menuId, dishId = dishId)
        val response = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/menu-digital-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toEntity(MenuDigitalCardResponse::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.menuId).isEqualTo(menuId)
        assertThat(response.body?.dishId).isEqualTo(dishId)
    }

    @Test
    @DisplayName("POST duplicate menu-dish assignment returns 409")
    fun duplicateAssignmentReturns409() {
        val request = CreateMenuDigitalCardRequest(menuId = menuId, dishId = dishId)
        noErrorRestClient()
            .post()
            .uri("/api/v1/admin/menu-digital-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toBodilessEntity()

        val duplicate = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/menu-digital-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toEntity(Map::class.java)

        assertThat(duplicate.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("GET menu-digital-cards by menuId returns assignments")
    fun getByMenuId() {
        val request = CreateMenuDigitalCardRequest(menuId = menuId, dishId = dishId)
        noErrorRestClient()
            .post()
            .uri("/api/v1/admin/menu-digital-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toBodilessEntity()

        val response = noErrorRestClient()
            .get()
            .uri("/api/v1/admin/menu-digital-cards/$menuId")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<List<MenuDigitalCardResponse>>() {})

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
        assertThat(response.body!![0].dishId).isEqualTo(dishId)
    }

    @Test
    @DisplayName("DELETE menu-digital-card assignment returns 204")
    fun deleteAssignment() {
        val request = CreateMenuDigitalCardRequest(menuId = menuId, dishId = dishId)
        val created = noErrorRestClient()
            .post()
            .uri("/api/v1/admin/menu-digital-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .retrieve()
            .toEntity(MenuDigitalCardResponse::class.java)
        val id = created.body!!.id

        val deleted = noErrorRestClient()
            .delete()
            .uri("/api/v1/admin/menu-digital-cards/$id")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toBodilessEntity()

        assertThat(deleted.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
