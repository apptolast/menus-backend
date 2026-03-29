package com.apptolast.menus.auth.dto.request

import jakarta.validation.constraints.NotBlank

data class GoogleCallbackRequest(
    @field:NotBlank val idToken: String
)
