package com.apptolast.menus.gdpr.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class RectificationRequest(
    @field:Email(message = "Must be a valid email address")
    @field:Size(max = 255, message = "Email must be at most 255 characters")
    val email: String? = null,

    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String? = null
)
