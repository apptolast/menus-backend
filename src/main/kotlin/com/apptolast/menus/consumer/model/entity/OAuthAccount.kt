package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "oauth_account",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])]
)
class OAuthAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "provider", nullable = false, length = 20)
    val provider: String = "",

    @Column(name = "provider_id", nullable = false, length = 255)
    val providerId: String = "",

    @Column(name = "email", nullable = false, columnDefinition = "BYTEA")
    val email: ByteArray = ByteArray(0)
)
