package com.apptolast.menus.restaurant.model.entity

import com.apptolast.menus.restaurant.model.enum.SubscriptionTier
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "subscription")
class Subscription(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false, unique = true)
    val restaurantId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    var tier: SubscriptionTier = SubscriptionTier.BASIC,

    @Column(name = "max_menus", nullable = false)
    var maxMenus: Int = 1,

    @Column(name = "max_dishes", nullable = false)
    var maxDishes: Int = 50,

    @Column(name = "starts_at", nullable = false)
    val startsAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
