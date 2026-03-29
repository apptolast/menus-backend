package com.apptolast.menus.restaurant.controller

import com.apptolast.menus.restaurant.dto.request.RestaurantRequest
import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.dto.PageResponse
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
@RequestMapping("/api/v1/admin/restaurants")
@Tag(name = "Admin - Restaurants", description = "Admin CRUD for restaurant management")
@SecurityRequirement(name = "Bearer Authentication")
class AdminRestaurantController(
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "List all restaurants (paginated)", description = "Optionally filter by name")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of restaurants returned"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    )
    fun listRestaurants(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<RestaurantResponse>> =
        ResponseEntity.ok(restaurantService.findAll(name, page, size))

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Restaurant found"),
        ApiResponse(responseCode = "404", description = "Restaurant not found")
    )
    fun getRestaurant(@PathVariable id: UUID): ResponseEntity<RestaurantResponse> =
        ResponseEntity.ok(restaurantService.findById(id))

    @PostMapping
    @Operation(summary = "Create a new restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Restaurant created"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "409", description = "Slug already in use")
    )
    fun createRestaurant(
        @Valid @RequestBody request: RestaurantRequest
    ): ResponseEntity<RestaurantResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(restaurantService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Update restaurant")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Restaurant updated"),
        ApiResponse(responseCode = "400", description = "Validation failed"),
        ApiResponse(responseCode = "404", description = "Restaurant not found")
    )
    fun updateRestaurant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RestaurantRequest
    ): ResponseEntity<RestaurantResponse> =
        ResponseEntity.ok(restaurantService.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete restaurant (set active=false)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Restaurant deactivated"),
        ApiResponse(responseCode = "404", description = "Restaurant not found")
    )
    fun deleteRestaurant(@PathVariable id: UUID): ResponseEntity<Void> {
        restaurantService.deactivate(id)
        return ResponseEntity.noContent().build()
    }
}
