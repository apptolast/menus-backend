package com.apptolast.menus.menu.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "menu")
class Menu(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "is_published", nullable = false)
    var isPublished: Boolean = false,

    @Column(name = "valid_from")
    var validFrom: OffsetDateTime? = null,

    @Column(name = "valid_to")
    var validTo: OffsetDateTime? = null,

    @Column(name = "client_name", length = 255)
    var clientName: String? = null,

    @Column(name = "client_logo_url", length = 500)
    var clientLogoUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "menu", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val sections: MutableList<MenuSection> = mutableListOf()
)
