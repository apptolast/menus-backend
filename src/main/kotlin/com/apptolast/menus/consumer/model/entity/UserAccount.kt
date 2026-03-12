package com.apptolast.menus.consumer.model.entity

import com.apptolast.menus.consumer.model.enum.UserRole
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class UserAccount(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String = "",

    @Column(name = "password_hash", length = 255)
    val passwordHash: String? = null,

    @Column(name = "name", length = 255)
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Column(name = "gdpr_consent", nullable = false)
    var gdprConsent: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
