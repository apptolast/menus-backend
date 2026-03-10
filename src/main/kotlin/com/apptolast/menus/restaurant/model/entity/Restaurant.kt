package com.apptolast.menus.restaurant.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "restaurant")
class Restaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false, unique = true)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    var slug: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "address", columnDefinition = "TEXT")
    var address: String? = null,

    @Column(name = "phone", length = 30)
    var phone: String? = null,

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
