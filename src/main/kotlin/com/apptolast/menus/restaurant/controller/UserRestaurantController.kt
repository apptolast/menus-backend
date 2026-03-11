package com.apptolast.menus.restaurant.controller

import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.restaurant.dto.request.CreateRestaurantRequest
import com.apptolast.menus.restaurant.dto.response.CreateRestaurantResponse
import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.restaurant.service.RestaurantCreationService
import com.apptolast.menus.shared.security.JwtTokenProvider
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User - Restaurant", description = "Self-service restaurant creation")
@SecurityRequirement(name = "Bearer Authentication")
class UserRestaurantController(
    private val restaurantCreationService: RestaurantCreationService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @PostMapping("/create-restaurant")
    @Operation(
        summary = "Create restaurant and become RESTAURANT_OWNER",
        description = "Upgrades CONSUMER to RESTAURANT_OWNER, creates restaurant + BASIC subscription, returns fresh JWT with tenantId"
    )
    @ApiResponse(responseCode = "201", description = "Restaurant created, role upgraded")
    fun createRestaurant(
        @Valid @RequestBody request: CreateRestaurantRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<CreateRestaurantResponse> {
        val result = restaurantCreationService.createRestaurantForCurrentUser(
            userId = principal.userId,
            profileUuid = principal.profileUuid,
            currentRole = principal.role,
            name = request.restaurantName,
            slug = request.slug
        )
        val restaurantResponse = RestaurantResponse(
            id = result.restaurant.id,
            name = result.restaurant.name,
            slug = result.restaurant.slug,
            description = result.restaurant.description,
            address = result.restaurant.address,
            phone = result.restaurant.phone,
            logoUrl = result.restaurant.logoUrl,
            isActive = result.restaurant.isActive,
            createdAt = result.restaurant.createdAt
        )
        val authResponse = AuthResponse(
            accessToken = jwtTokenProvider.generateAccessToken(
                result.user.id, result.user.role.name, result.user.profileUuid, result.tenantId
            ),
            refreshToken = jwtTokenProvider.generateRefreshToken(result.user.id)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateRestaurantResponse(restaurant = restaurantResponse, auth = authResponse)
        )
    }
}
