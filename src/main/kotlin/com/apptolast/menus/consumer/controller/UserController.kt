package com.apptolast.menus.consumer.controller

import com.apptolast.menus.auth.service.ConsentService
import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import com.apptolast.menus.consumer.service.UserAllergenProfileService
import com.apptolast.menus.shared.exception.ConsentRequiredException
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
    private val userAllergenProfileService: UserAllergenProfileService,
    private val consentService: ConsentService
) {

    @GetMapping("/allergen-profile")
    @Operation(summary = "Get my allergen profile (requires GDPR consent)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen profile retrieved"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "GDPR consent required"),
        ApiResponse(responseCode = "404", description = "Profile not found")
    )
    fun getAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<AllergenProfileResponse> {
        requireConsent(principal)
        return ResponseEntity.ok(userAllergenProfileService.getProfile(principal.profileUuid))
    }

    @PutMapping("/allergen-profile")
    @Operation(summary = "Update my allergen profile (requires GDPR consent)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Allergen profile updated"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "GDPR consent required")
    )
    fun updateAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AllergenProfileRequest
    ): ResponseEntity<AllergenProfileResponse> {
        requireConsent(principal)
        return ResponseEntity.ok(
            userAllergenProfileService.upsertProfile(principal.profileUuid, request)
        )
    }

    @DeleteMapping("/allergen-profile")
    @Operation(
        summary = "Delete my allergen profile (requires GDPR consent)",
        description = "Physically deletes the user_allergen_profile record. This is different from revoking consent, which also deletes the profile but additionally revokes the consent record."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Allergen profile deleted"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "GDPR consent required")
    )
    fun deleteAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        requireConsent(principal)
        userAllergenProfileService.deleteProfile(principal.profileUuid)
        return ResponseEntity.noContent().build()
    }

    private fun requireConsent(principal: UserPrincipal) {
        if (!consentService.hasActiveConsent(principal.profileUuid)) {
            throw ConsentRequiredException()
        }
    }
}
