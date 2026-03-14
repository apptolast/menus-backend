package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurant
import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurantId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserFavoriteRestaurantRepository : JpaRepository<UserFavoriteRestaurant, UserFavoriteRestaurantId> {

    @Query("SELECT uf FROM UserFavoriteRestaurant uf JOIN FETCH uf.restaurant WHERE uf.user.id = :userId")
    fun findByUserIdWithRestaurant(userId: UUID): List<UserFavoriteRestaurant>

    fun existsByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID): Boolean
    fun deleteByUserIdAndRestaurantId(userId: UUID, restaurantId: UUID)
}
