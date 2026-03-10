package com.apptolast.menus.consumer.controller

import com.apptolast.menus.auth.service.ConsentService
import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import com.apptolast.menus.consumer.service.UserAllergenProfileService
import com.apptolast.menus.shared.exception.ConsentRequiredException
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
    fun getAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<AllergenProfileResponse> {
        requireConsent(principal)
        return ResponseEntity.ok(userAllergenProfileService.getProfile(principal.profileUuid))
    }

    @PutMapping("/allergen-profile")
    @Operation(summary = "Update my allergen profile (requires GDPR consent)")
    fun updateAllergenProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody request: AllergenProfileRequest
    ): ResponseEntity<AllergenProfileResponse> {
        requireConsent(principal)
        return ResponseEntity.ok(
            userAllergenProfileService.upsertProfile(principal.profileUuid, request)
        )
    }

    private fun requireConsent(principal: UserPrincipal) {
        if (!consentService.hasActiveConsent(principal.profileUuid)) {
            throw ConsentRequiredException()
        }
    }
}
