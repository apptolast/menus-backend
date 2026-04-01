package com.apptolast.menus.consumer.service

import com.apptolast.menus.consumer.dto.response.FavoriteRestaurantResponse
import java.util.UUID

interface FavoriteRestaurantService {
    fun getFavorites(userId: UUID): List<FavoriteRestaurantResponse>
    fun addFavorite(userId: UUID, restaurantId: UUID)
    fun removeFavorite(userId: UUID, restaurantId: UUID)
    fun isFavorite(userId: UUID, restaurantId: UUID): Boolean
}
