package com.apptolast.menus.consumer.model.entity

import com.apptolast.menus.consumer.model.enum.UserRole
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_account")
class UserAccount(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "BYTEA")
    val email: ByteArray = ByteArray(0),

    @Column(name = "email_hash", nullable = false, unique = true, length = 64)
    val emailHash: String = "",

    @Column(name = "password_hash", length = 255)
    val passwordHash: String? = null,

    @Column(name = "profile_uuid", nullable = false, unique = true)
    val profileUuid: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    val role: UserRole = UserRole.CONSUMER,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
