package com.apptolast.menus.recipe.model.entity

import com.apptolast.menus.ingredient.model.entity.Ingredient
import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class RecipeIngredientId(
    val recipe: UUID? = null,
    val ingredient: UUID? = null
) : Serializable

@Entity
@Table(name = "recipe_ingredients")
@IdClass(RecipeIngredientId::class)
class RecipeIngredient(
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe = Recipe(),

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @Column(name = "quantity", precision = 10, scale = 3)
    var quantity: BigDecimal? = null,

    @Column(name = "unit", length = 30)
    var unit: String? = null
)
