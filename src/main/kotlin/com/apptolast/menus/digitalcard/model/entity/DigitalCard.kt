package com.apptolast.menus.digitalcard.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "digital_cards")
class DigitalCard(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "restaurant_id", nullable = false)
    val restaurantId: UUID,

    @Column(name = "menu_id", nullable = false)
    val menuId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    var slug: String,

    @Column(name = "qr_code_url", length = 500)
    var qrCodeUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "custom_css", columnDefinition = "jsonb")
    var customCss: String? = "{}",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    constructor() : this(
        restaurantId = UUID.randomUUID(),
        menuId = UUID.randomUUID(),
        tenantId = UUID.randomUUID(),
        slug = ""
    )
}
