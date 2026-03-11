package com.apptolast.menus.restaurant.dto.response

import com.apptolast.menus.auth.dto.response.AuthResponse

data class CreateRestaurantResponse(
    val restaurant: RestaurantResponse,
    val auth: AuthResponse
)
