package com.apptolast.menus.ingredient.service

import com.apptolast.menus.ingredient.model.entity.Ingredient
import java.util.UUID

interface IngredientService {
    fun findAll(): List<Ingredient>
    fun findById(id: UUID): Ingredient
    fun create(ingredient: Ingredient): Ingredient
    fun update(id: UUID, ingredient: Ingredient): Ingredient
    fun delete(id: UUID)
    fun searchByName(name: String): List<Ingredient>
}
