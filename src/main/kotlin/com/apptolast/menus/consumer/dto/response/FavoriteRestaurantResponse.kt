package com.apptolast.menus.consumer.dto.response

import java.util.UUID

data class FavoriteRestaurantResponse(
    val restaurantId: UUID,
    val restaurantName: String,
    val restaurantSlug: String
)
