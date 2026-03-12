package com.apptolast.menus.digitalcard.dto.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateDigitalCardRequest(
    @field:Size(max = 100, message = "Slug must be at most 100 characters")
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug must be lowercase alphanumeric with hyphens"
    )
    val slug: String? = null,

    val isActive: Boolean? = null,

    val customCss: String? = null
)
