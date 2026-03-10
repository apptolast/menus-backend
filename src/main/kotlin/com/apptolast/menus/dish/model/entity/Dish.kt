package com.apptolast.menus.dish.model.entity

import com.apptolast.menus.menu.model.entity.MenuSection
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "dish")
class Dish(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    val section: MenuSection = MenuSection(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "price", precision = 10, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "dish", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val allergens: MutableList<DishAllergen> = mutableListOf()
)
