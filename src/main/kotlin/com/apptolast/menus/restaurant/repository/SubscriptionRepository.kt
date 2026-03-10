package com.apptolast.menus.restaurant.repository

import com.apptolast.menus.restaurant.model.entity.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface SubscriptionRepository : JpaRepository<Subscription, UUID> {
    fun findByRestaurantId(restaurantId: UUID): Optional<Subscription>
}
