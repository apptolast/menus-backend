package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "consent_record")
class ConsentRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "profile_uuid", nullable = false)
    val profileUuid: UUID = UUID.randomUUID(),

    @Column(name = "consent_type", nullable = false, length = 50)
    val consentType: String = "",

    @Column(name = "granted", nullable = false)
    val granted: Boolean = false,

    @Column(name = "ip_address", columnDefinition = "INET")
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null
)
