package com.apptolast.menus.consumer.service

import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import java.util.UUID

interface FavoriteRestaurantService {
    fun getFavorites(userId: UUID): List<RestaurantResponse>
    fun addFavorite(userId: UUID, restaurantId: UUID)
    fun removeFavorite(userId: UUID, restaurantId: UUID)
    fun isFavorite(userId: UUID, restaurantId: UUID): Boolean
}
