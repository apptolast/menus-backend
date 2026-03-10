package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.ConsentRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.UUID

interface ConsentRecordRepository : JpaRepository<ConsentRecord, UUID> {
    fun findByProfileUuidAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
        profileUuid: UUID,
        consentType: String
    ): ConsentRecord?

    @Modifying
    @Query("UPDATE ConsentRecord c SET c.revokedAt = :now WHERE c.profileUuid = :profileUuid AND c.revokedAt IS NULL")
    fun revokeAllByProfileUuid(profileUuid: UUID, now: OffsetDateTime)
}
