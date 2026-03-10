package com.apptolast.menus.restaurant.repository

import com.apptolast.menus.restaurant.model.entity.Restaurant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface RestaurantRepository : JpaRepository<Restaurant, UUID> {
    fun findBySlug(slug: String): Optional<Restaurant>
    fun findByOwnerId(ownerId: UUID): Optional<Restaurant>

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findActiveByNameContaining(name: String?, pageable: Pageable): Page<Restaurant>
}
