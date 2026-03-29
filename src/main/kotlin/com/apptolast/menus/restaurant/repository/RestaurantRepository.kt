package com.apptolast.menus.restaurant.repository

import com.apptolast.menus.restaurant.model.entity.Restaurant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RestaurantRepository : JpaRepository<Restaurant, UUID> {
    fun findBySlug(slug: String): Restaurant?
    fun existsBySlug(slug: String): Boolean
    fun findByActiveTrue(pageable: Pageable): Page<Restaurant>
    @Query("SELECT r FROM Restaurant r WHERE r.active = true AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findActiveByNameContaining(name: String, pageable: Pageable): Page<Restaurant>
}
