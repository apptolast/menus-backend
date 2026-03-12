package com.apptolast.menus.ingredient.repository

import com.apptolast.menus.ingredient.model.entity.Ingredient
import java.util.UUID

interface IngredientRepository {
    fun findById(id: UUID): Ingredient?
    fun findAllByTenantId(tenantId: UUID): List<Ingredient>
    fun save(entity: Ingredient): Ingredient
    fun deleteById(id: UUID)
    fun existsByNameAndTenantId(name: String, tenantId: UUID): Boolean
    fun searchByName(name: String, tenantId: UUID): List<Ingredient>
}
