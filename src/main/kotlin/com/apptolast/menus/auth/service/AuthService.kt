package com.apptolast.menus.auth.service

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse

interface AuthService {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun refresh(refreshToken: String): AuthResponse
    fun loginWithGoogle(idToken: String): AuthResponse
}
