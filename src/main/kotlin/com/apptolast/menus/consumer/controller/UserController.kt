package com.apptolast.menus.consumer.controller

import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import com.apptolast.menus.consumer.service.UserAllergenProfileService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User Profile", description = "Manage user allergen profile")
@SecurityRequirement(name = "Bearer Authentication")
class UserController(
    private val userAllergenProfileService: UserAllergenProfileService
) {

    @GetMapping("/allergen-profile")
    @Operation(summary = "Get my allergen profile")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen profile retrieved"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "404", description = "Profile not found")
    )
    fun getAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<AllergenProfileResponse> =
        ResponseEntity.ok(userAllergenProfileService.getProfile(principal.userId))

    @PutMapping("/allergen-profile")
    @Operation(summary = "Update my allergen profile")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen profile updated"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun updateAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AllergenProfileRequest
    ): ResponseEntity<AllergenProfileResponse> =
        ResponseEntity.ok(userAllergenProfileService.upsertProfile(principal.userId, request))

    @DeleteMapping("/allergen-profile")
    @Operation(summary = "Delete my allergen profile")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Allergen profile deleted"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun deleteAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        userAllergenProfileService.deleteProfile(principal.userId)
        return ResponseEntity.noContent().build()
    }
}
