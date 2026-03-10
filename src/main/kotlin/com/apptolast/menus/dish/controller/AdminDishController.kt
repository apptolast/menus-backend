package com.apptolast.menus.dish.controller

import com.apptolast.menus.dish.dto.request.DishAllergenRequest
import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.dish.service.DishService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/dishes")
@Tag(name = "Admin - Dishes", description = "Dish and allergen management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class AdminDishController(
    private val dishService: DishService,
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "List all dishes for own restaurant")
    fun listDishes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<DishResponse>> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(dishService.findByRestaurant(restaurant.id))
    }

    @PostMapping
    @Operation(summary = "Create a dish with allergens")
    fun createDish(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: DishRequest
    ): ResponseEntity<DishResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(dishService.create(restaurant.tenantId, request))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a dish")
    fun updateDish(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DishRequest
    ): ResponseEntity<DishResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(dishService.update(id, restaurant.tenantId, request))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dish")
    fun deleteDish(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        dishService.delete(id, restaurant.tenantId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/allergens")
    @Operation(summary = "Add or update an allergen on a dish")
    fun addAllergen(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DishAllergenRequest
    ): ResponseEntity<DishResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(
            dishService.addAllergen(id, restaurant.tenantId, principal.profileUuid, request)
        )
    }

    @DeleteMapping("/{id}/allergens/{allergenId}")
    @Operation(summary = "Remove an allergen from a dish")
    fun removeAllergen(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @PathVariable allergenId: Int
    ): ResponseEntity<Void> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        dishService.removeAllergen(id, allergenId, restaurant.tenantId, principal.profileUuid)
        return ResponseEntity.noContent().build()
    }
}
