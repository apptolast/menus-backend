package com.apptolast.menus.dashboard.controller

import com.apptolast.menus.dashboard.dto.response.DashboardStatsResponse
import com.apptolast.menus.dashboard.service.DashboardService
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin - Dashboard", description = "Restaurant dashboard statistics")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
class DashboardController(
    private val dashboardService: DashboardService,
    private val restaurantService: RestaurantService
) {

    @GetMapping("/stats")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Returns aggregated statistics for the restaurant including ingredient counts, recipe counts, menu status, and allergen distribution"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Statistics returned"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Not authorized")
    )
    fun getStats(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<DashboardStatsResponse> {
        val restaurant = restaurantService.findByOwnerId(principal.userId)
        return ResponseEntity.ok(dashboardService.getStats(restaurant.id))
    }
}
