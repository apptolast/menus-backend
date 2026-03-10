package com.apptolast.menus.auth.controller

import com.apptolast.menus.auth.dto.request.GoogleCallbackRequest
import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RefreshTokenRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.service.AuthService
import com.apptolast.menus.auth.service.ConsentService
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh tokens, Google OAuth2")
class AuthController(
    private val authService: AuthService,
    private val consentService: ConsentService
) {

    @PostMapping("/register")
    @Operation(summary = "Register with email and password")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request.refreshToken))

    @PostMapping("/oauth2/google/callback")
    @Operation(summary = "Login or register via Google ID token")
    fun googleCallback(@Valid @RequestBody request: GoogleCallbackRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.loginWithGoogle(request.idToken))

    @PostMapping("/consent")
    @Operation(summary = "Grant GDPR consent for health data processing (Art. 9)")
    fun grantConsent(
        @AuthenticationPrincipal principal: UserPrincipal,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {
        consentService.grantConsent(
            profileUuid = principal.profileUuid,
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )
        return ResponseEntity.ok(mapOf("message" to "Consent granted successfully"))
    }

    @DeleteMapping("/consent")
    @Operation(summary = "Revoke GDPR consent and delete allergen profile")
    fun revokeConsent(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        consentService.revokeConsent(principal.profileUuid)
        return ResponseEntity.noContent().build()
    }
}
