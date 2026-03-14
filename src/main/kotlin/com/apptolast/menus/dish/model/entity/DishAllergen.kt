package com.apptolast.menus.dish.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

data class DishAllergenId(
    val dish: UUID? = null,
    val allergen: Int? = null
) : Serializable

@Entity
@Table(name = "dish_allergens")
@IdClass(DishAllergenId::class)
class DishAllergen(
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dish_id", nullable = false)
    val dish: Dish = Dish(),

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
)
