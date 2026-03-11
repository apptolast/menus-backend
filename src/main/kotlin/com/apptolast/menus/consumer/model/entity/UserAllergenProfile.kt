package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_allergen_profile")
class UserAllergenProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "profile_uuid", nullable = false, unique = true)
    val profileUuid: UUID = UUID.randomUUID(),

    @Column(name = "allergen_codes", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    var allergenCodes: List<String> = emptyList(),

    @Column(name = "severity_notes", columnDefinition = "TEXT")
    var severityNotes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
