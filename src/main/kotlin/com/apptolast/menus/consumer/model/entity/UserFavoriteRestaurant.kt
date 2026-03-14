package com.apptolast.menus.consumer.model.entity

import com.apptolast.menus.restaurant.model.entity.Restaurant
import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class UserFavoriteRestaurantId(
    val user: UUID? = null,
    val restaurant: UUID? = null
) : Serializable

@Entity
@Table(name = "user_favorite_restaurants")
@IdClass(UserFavoriteRestaurantId::class)
class UserFavoriteRestaurant(
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserAccount = UserAccount(),

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", nullable = false)
    val restaurant: Restaurant = Restaurant(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
