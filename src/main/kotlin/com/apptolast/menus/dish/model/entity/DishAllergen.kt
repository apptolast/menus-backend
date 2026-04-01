package com.apptolast.menus.dish.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "dish_allergens",
    uniqueConstraints = [UniqueConstraint(columnNames = ["dish_id", "allergen_id"])]
)
class DishAllergen(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dish_id", nullable = false)
    val dish: Dish = Dish(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
)
