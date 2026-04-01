package com.apptolast.menus.recipe.model.entity

import com.apptolast.menus.ingredient.model.entity.Ingredient
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(
    name = "recipe_ingredients",
    uniqueConstraints = [UniqueConstraint(columnNames = ["recipe_id", "ingredient_id"])]
)
class RecipeIngredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe = Recipe(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @Column(name = "quantity", precision = 10, scale = 3)
    var quantity: BigDecimal? = null,

    @Column(name = "unit", length = 30)
    var unit: String? = null
)
