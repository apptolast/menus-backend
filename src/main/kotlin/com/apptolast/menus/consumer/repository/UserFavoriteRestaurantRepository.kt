package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurant
import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurantId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserFavoriteRestaurantRepository : JpaRepository<UserFavoriteRestaurant, UserFavoriteRestaurantId> {
    fun findByUserId(userId: UUID): List<UserFavoriteRestaurant>
    fun existsByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID): Boolean
    fun deleteByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID)
}
