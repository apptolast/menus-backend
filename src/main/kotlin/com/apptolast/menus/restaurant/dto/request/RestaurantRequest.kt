package com.apptolast.menus.restaurant.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RestaurantRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 100) val slug: String,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val logoUrl: String? = null
)
