package com.apptolast.menus.ingredient.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ingredients")
class Ingredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "brand", length = 255)
    var brand: String? = null,

    @Column(name = "label_info", columnDefinition = "TEXT")
    var labelInfo: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "ingredient", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val allergens: MutableList<IngredientAllergen> = mutableListOf()
)
