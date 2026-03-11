package com.apptolast.menus.restaurant.service

import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.restaurant.model.entity.Restaurant
import java.util.UUID

data class RestaurantCreationResult(
    val restaurant: Restaurant,
    val user: UserAccount,
    val tenantId: UUID
)

interface RestaurantCreationService {
    fun createRestaurantForCurrentUser(
        userId: UUID,
        profileUuid: UUID,
        currentRole: UserRole,
        name: String,
        slug: String?
    ): RestaurantCreationResult
}
