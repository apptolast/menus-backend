package com.apptolast.menus.dish.controller

import com.apptolast.menus.dish.dto.request.DishAllergenRequest
import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.dish.service.DishService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@Tag(name = "Admin - Dishes", description = "Dish and allergen management")
@SecurityRequirement(name = "Bearer Authentication")
class AdminDishController(
    private val dishService: DishService
) {

    @GetMapping("/api/v1/admin/restaurants/{restaurantId}/dishes")
    @Operation(summary = "List all dishes for a restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of dishes"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    )
    fun listDishes(
        @PathVariable restaurantId: UUID
    ): ResponseEntity<List<DishResponse>> =
        ResponseEntity.ok(dishService.findByRestaurant(restaurantId))

    @PostMapping("/api/v1/admin/dishes")
    @Operation(summary = "Create a dish with allergens")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Dish created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Section not found")
    )
    fun createDish(
        @Valid @RequestBody request: DishRequest
    ): ResponseEntity<DishResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(dishService.create(request))

    @PutMapping("/api/v1/admin/dishes/{id}")
    @Operation(summary = "Update a dish")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Dish updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Dish not found")
    )
    fun updateDish(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DishRequest
    ): ResponseEntity<DishResponse> =
        ResponseEntity.ok(dishService.update(id, request))

    @DeleteMapping("/api/v1/admin/dishes/{id}")
    @Operation(summary = "Delete a dish")
    @ApiResponse(responseCode = "204", description = "Dish deleted")
    fun deleteDish(@PathVariable id: UUID): ResponseEntity<Void> {
        dishService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/v1/admin/dishes/{id}/allergens")
    @Operation(summary = "Add or update an allergen on a dish")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen added or updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Dish or allergen not found")
    )
    fun addAllergen(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DishAllergenRequest
    ): ResponseEntity<DishResponse> =
        ResponseEntity.ok(dishService.addAllergen(id, request))

    @DeleteMapping("/api/v1/admin/dishes/{id}/allergens/{allergenId}")
    @Operation(summary = "Remove an allergen from a dish")
    @ApiResponse(responseCode = "204", description = "Allergen removed")
    fun removeAllergen(
        @PathVariable id: UUID,
        @PathVariable allergenId: Int
    ): ResponseEntity<Void> {
        dishService.removeAllergen(id, allergenId)
        return ResponseEntity.noContent().build()
    }
}
