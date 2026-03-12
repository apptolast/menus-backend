package com.apptolast.menus.recipe.model.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "recipes")
class Recipe(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "category", length = 100)
    var category: String? = null,

    @Column(name = "is_sub_elaboration", nullable = false)
    var isSubElaboration: Boolean = false,

    @Column(name = "price", precision = 10, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "recipe", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val ingredients: MutableList<RecipeIngredient> = mutableListOf()
) {
    constructor() : this(tenantId = UUID.randomUUID(), restaurantId = UUID.randomUUID(), name = "")
}
