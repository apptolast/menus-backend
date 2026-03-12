package com.apptolast.menus.ingredient.repository

import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.repository.jpa.JpaIngredientRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class IngredientRepositoryAdapter(
    private val jpa: JpaIngredientRepository
) : IngredientRepository {
    override fun findById(id: UUID): Ingredient? = jpa.findById(id).orElse(null)
    override fun findAllByTenantId(tenantId: UUID): List<Ingredient> = jpa.findAllByTenantId(tenantId)
    override fun save(entity: Ingredient): Ingredient = jpa.save(entity)
    override fun deleteById(id: UUID) = jpa.deleteById(id)
    override fun existsByNameAndTenantId(name: String, tenantId: UUID): Boolean = jpa.existsByNameAndTenantId(name, tenantId)
    override fun searchByName(name: String, tenantId: UUID): List<Ingredient> = jpa.searchByName(name, tenantId)
}
