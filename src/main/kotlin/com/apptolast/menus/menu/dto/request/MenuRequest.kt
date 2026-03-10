package com.apptolast.menus.menu.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MenuRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    val description: String? = null,
    val displayOrder: Int = 0
)
