package com.apptolast.menus.auth.service

import com.apptolast.menus.consumer.model.entity.ConsentRecord
import com.apptolast.menus.consumer.repository.ConsentRecordRepository
import com.apptolast.menus.consumer.repository.UserAllergenProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class ConsentService(
    private val consentRecordRepository: ConsentRecordRepository,
    private val userAllergenProfileRepository: UserAllergenProfileRepository
) {
    companion object {
        const val HEALTH_DATA_CONSENT = "HEALTH_DATA_PROCESSING"
    }

    fun hasActiveConsent(profileUuid: UUID): Boolean =
        consentRecordRepository.findByProfileUuidAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
            profileUuid, HEALTH_DATA_CONSENT
        ) != null

    fun grantConsent(profileUuid: UUID, ipAddress: String?, userAgent: String?) {
        val existing = consentRecordRepository.findByProfileUuidAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
            profileUuid, HEALTH_DATA_CONSENT
        )
        if (existing != null) {
            return
        }
        val record = ConsentRecord(
            profileUuid = profileUuid,
            consentType = HEALTH_DATA_CONSENT,
            granted = true,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        consentRecordRepository.save(record)
    }

    fun revokeConsent(profileUuid: UUID) {
        consentRecordRepository.revokeAllByProfileUuid(profileUuid, OffsetDateTime.now())
        userAllergenProfileRepository.deleteByProfileUuid(profileUuid)
    }
}
