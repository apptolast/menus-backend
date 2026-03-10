package com.apptolast.menus.menu.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SectionRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    val displayOrder: Int = 0
)
