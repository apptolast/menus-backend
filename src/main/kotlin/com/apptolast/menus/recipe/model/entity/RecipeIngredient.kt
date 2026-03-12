package com.apptolast.menus.recipe.model.entity

import com.apptolast.menus.ingredient.model.entity.Ingredient
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "recipe_ingredients")
class RecipeIngredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    val recipe: Recipe = Recipe(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    var ingredient: Ingredient? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    var subRecipe: Recipe? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "quantity", precision = 10, scale = 3)
    var quantity: BigDecimal? = null,

    @Column(name = "unit", length = 50)
    var unit: String? = null,

    @Column(name = "notes", length = 500)
    var notes: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
) {
    constructor() : this(id = UUID.randomUUID())
}
