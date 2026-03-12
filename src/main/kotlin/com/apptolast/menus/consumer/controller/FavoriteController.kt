package com.apptolast.menus.consumer.controller

import com.apptolast.menus.consumer.dto.response.FavoriteRestaurantResponse
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurant
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.consumer.repository.UserFavoriteRestaurantRepository
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@Tag(name = "User Favorites", description = "Manage favorite restaurants")
@SecurityRequirement(name = "Bearer Authentication")
class FavoriteController(
    private val userFavoriteRestaurantRepository: UserFavoriteRestaurantRepository,
    private val userAccountRepository: UserAccountRepository,
    private val restaurantRepository: RestaurantRepository
) {

    @GetMapping
    @Operation(summary = "List favorite restaurants")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of favorite restaurants"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun listFavorites(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<FavoriteRestaurantResponse>> {
        val favorites = userFavoriteRestaurantRepository.findByUserId(principal.userId)
        return ResponseEntity.ok(favorites.map { it.toResponse() })
    }

    @PostMapping("/{restaurantId}")
    @Transactional
    @Operation(summary = "Add restaurant to favorites")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Restaurant added to favorites"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Restaurant not found"),
        ApiResponse(responseCode = "409", description = "Already in favorites")
    )
    fun addFavorite(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable restaurantId: UUID
    ): ResponseEntity<Void> {
        if (userFavoriteRestaurantRepository.existsByUserIdAndRestaurantId(principal.userId, restaurantId)) {
            throw ConflictException("ALREADY_FAVORITE", "Restaurant is already in favorites")
        }
        val user = userAccountRepository.findById(principal.userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("RESTAURANT_NOT_FOUND", "Restaurant not found") }
        userFavoriteRestaurantRepository.save(UserFavoriteRestaurant(user = user, restaurant = restaurant))
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{restaurantId}")
    @Transactional
    @Operation(summary = "Remove restaurant from favorites")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Restaurant removed from favorites"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun removeFavorite(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable restaurantId: UUID
    ): ResponseEntity<Void> {
        userFavoriteRestaurantRepository.deleteByUserIdAndRestaurantId(principal.userId, restaurantId)
        return ResponseEntity.noContent().build()
    }

    private fun UserFavoriteRestaurant.toResponse() = FavoriteRestaurantResponse(
        restaurantId = restaurant.id,
        restaurantName = restaurant.name,
        restaurantSlug = restaurant.slug
    )
}
