package com.apptolast.menus.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class WhitelistRequest(
    @field:NotBlank
    @field:Email
    val email: String
)
