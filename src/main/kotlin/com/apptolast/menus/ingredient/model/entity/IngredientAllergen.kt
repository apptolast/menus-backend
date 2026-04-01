package com.apptolast.menus.ingredient.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "ingredient_allergens",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ingredient_id", "allergen_id"])]
)
class IngredientAllergen(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient = Ingredient(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS
)
