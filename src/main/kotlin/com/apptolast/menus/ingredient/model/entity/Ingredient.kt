package com.apptolast.menus.ingredient.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ingredients")
class Ingredient(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "brand", length = 255)
    var brand: String? = null,

    @Column(name = "supplier", length = 255)
    var supplier: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergens", columnDefinition = "jsonb", nullable = false)
    var allergens: String = "[]",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "traces", columnDefinition = "jsonb")
    var traces: String? = "[]",

    @Column(name = "ocr_raw_text", columnDefinition = "TEXT")
    var ocrRawText: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    constructor() : this(tenantId = UUID.randomUUID(), name = "")
}
