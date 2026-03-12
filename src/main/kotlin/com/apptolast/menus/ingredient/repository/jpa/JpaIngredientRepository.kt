package com.apptolast.menus.ingredient.repository.jpa

import com.apptolast.menus.ingredient.model.entity.Ingredient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface JpaIngredientRepository : JpaRepository<Ingredient, UUID> {
    fun findAllByTenantId(tenantId: UUID): List<Ingredient>
    fun existsByNameAndTenantId(name: String, tenantId: UUID): Boolean

    @Query("SELECT i FROM Ingredient i WHERE i.tenantId = :tenantId AND LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun searchByName(@Param("name") name: String, @Param("tenantId") tenantId: UUID): List<Ingredient>
}
