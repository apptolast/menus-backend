package com.apptolast.menus.auth.controller

import com.apptolast.menus.auth.dto.request.GoogleCallbackRequest
import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RefreshTokenRequest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh tokens, Google OAuth2")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user with email and password")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/register-admin")
    @Operation(
        summary = "Register a new admin user",
        description = "Creates an admin account. The email must be in the admin whitelist."
    )
    fun registerAdmin(@Valid @RequestBody request: RegisterAdminRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(request))

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request.refreshToken))

    @PostMapping("/oauth2/google/callback")
    @Operation(summary = "Login or register via Google ID token")
    fun googleCallback(@Valid @RequestBody request: GoogleCallbackRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.loginWithGoogle(request.idToken))
}
