package com.apptolast.menus.digitalcard.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateDigitalCardRequest(
    @field:NotNull(message = "Menu ID is required")
    val menuId: UUID,

    @field:NotBlank(message = "Slug is required")
    @field:Size(max = 100, message = "Slug must be at most 100 characters")
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug must be lowercase alphanumeric with hyphens"
    )
    val slug: String
)
