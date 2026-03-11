package com.apptolast.menus.audit.model.entity

import com.apptolast.menus.dish.model.enum.ContainmentLevel
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "allergen_audit_log")
class AllergenAuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "dish_id", nullable = false)
    val dishId: UUID = UUID.randomUUID(),

    @Column(name = "allergen_id", nullable = false)
    val allergenId: Int = 0,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID = UUID.randomUUID(),

    @Column(name = "changed_by_uuid", nullable = false)
    val changedByUuid: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    val action: AuditAction = AuditAction.ADD,

    @Enumerated(EnumType.STRING)
    @Column(name = "old_level", length = 20)
    val oldLevel: ContainmentLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_level", length = 20)
    val newLevel: ContainmentLevel? = null,

    @Column(name = "changed_at", nullable = false, updatable = false)
    val changedAt: OffsetDateTime = OffsetDateTime.now()
)

enum class AuditAction {
    ADD,
    REMOVE,
    UPDATE
}

