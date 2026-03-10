package com.apptolast.menus.restaurant.controller

import com.apptolast.menus.restaurant.dto.request.RestaurantRequest
import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/restaurant")
@Tag(name = "Admin - Restaurant", description = "Restaurant owner management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class AdminRestaurantController(
    private val restaurantService: RestaurantService
) {

    @GetMapping
    @Operation(summary = "Get own restaurant details")
    fun getOwnRestaurant(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<RestaurantResponse> =
        ResponseEntity.ok(restaurantService.findByOwnerId(principal.userId))

    @PutMapping
    @Operation(summary = "Update own restaurant")
    fun updateRestaurant(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: RestaurantRequest
    ): ResponseEntity<RestaurantResponse> =
        ResponseEntity.ok(restaurantService.update(principal.userId, request))
}
