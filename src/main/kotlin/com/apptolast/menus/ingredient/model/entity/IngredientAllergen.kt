package com.apptolast.menus.ingredient.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

data class IngredientAllergenId(
    val ingredient: UUID? = null,
    val allergen: Int? = null
) : Serializable

@Entity
@Table(name = "ingredient_allergens")
@IdClass(IngredientAllergenId::class)
class IngredientAllergen(
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS
)
