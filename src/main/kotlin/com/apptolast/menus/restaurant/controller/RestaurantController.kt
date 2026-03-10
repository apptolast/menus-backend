package com.apptolast.menus.restaurant.controller

import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants (Public)", description = "Public restaurant browsing endpoints")
class RestaurantController(
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "Search restaurants by name (paginated)")
    fun listRestaurants(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<RestaurantResponse>> =
        ResponseEntity.ok(restaurantService.findAll(name, page, size))

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID")
    fun getRestaurant(@PathVariable id: UUID): ResponseEntity<RestaurantResponse> =
        ResponseEntity.ok(restaurantService.findById(id))
}
