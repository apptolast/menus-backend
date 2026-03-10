package com.apptolast.menus.dish.model.entity

import com.apptolast.menus.allergen.model.entity.Allergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "dish_allergen",
    uniqueConstraints = [UniqueConstraint(columnNames = ["dish_id", "allergen_id"])]
)
class DishAllergen(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    val dish: Dish = Dish(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_id", nullable = false)
    val allergen: Allergen = Allergen(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "containment_level", nullable = false, length = 20)
    var containmentLevel: ContainmentLevel = ContainmentLevel.CONTAINS,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
)
