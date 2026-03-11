package com.apptolast.menus.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRestaurantRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val password: String,
    val acceptTerms: Boolean = false,
    @field:NotBlank @field:Size(max = 255) val restaurantName: String,
    @field:Size(max = 100) @field:Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") val slug: String? = null
)
