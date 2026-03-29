package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "consumer_allergen_profiles")
class UserAllergenProfile(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "allergen_codes", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    var allergenCodes: List<String> = emptyList(),

    @Column(name = "severity_notes", columnDefinition = "TEXT")
    var severityNotes: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
