package com.apptolast.menus.restaurant.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateRestaurantRequest(
    @field:NotBlank @field:Size(max = 255) val restaurantName: String,
    @field:Size(max = 100) @field:Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") val slug: String? = null
)
