package com.apptolast.menus.consumer.controller

import com.apptolast.menus.consumer.dto.response.FavoriteRestaurantResponse
import com.apptolast.menus.consumer.service.FavoriteRestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@Tag(name = "User Favorites", description = "Manage favorite restaurants")
@SecurityRequirement(name = "Bearer Authentication")
class FavoriteController(
    private val favoriteRestaurantService: FavoriteRestaurantService
) {

    @GetMapping
    @Operation(summary = "List favorite restaurants")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of favorite restaurants"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun listFavorites(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<FavoriteRestaurantResponse>> =
        ResponseEntity.ok(favoriteRestaurantService.getFavorites(principal.userId))

    @PostMapping("/{restaurantId}")
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
        favoriteRestaurantService.addFavorite(principal.userId, restaurantId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{restaurantId}")
    @Operation(summary = "Remove restaurant from favorites")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Restaurant removed from favorites"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun removeFavorite(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable restaurantId: UUID
    ): ResponseEntity<Void> {
        favoriteRestaurantService.removeFavorite(principal.userId, restaurantId)
        return ResponseEntity.noContent().build()
    }
}
