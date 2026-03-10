package com.apptolast.menus.gdpr.service.impl

import com.apptolast.menus.config.EncryptionConfig
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.repository.ConsentRecordRepository
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.consumer.repository.UserAllergenProfileRepository
import com.apptolast.menus.gdpr.dto.response.AllergenProfileExport
import com.apptolast.menus.gdpr.dto.response.DataExportResponse
import com.apptolast.menus.gdpr.service.GdprService
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class GdprServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val userAllergenProfileRepository: UserAllergenProfileRepository,
    private val consentRecordRepository: ConsentRecordRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val encryptionConfig: EncryptionConfig
) : GdprService {

    @Transactional(readOnly = true)
    override fun exportUserData(userId: UUID): DataExportResponse {
        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        val profile = userAllergenProfileRepository.findByProfileUuid(user.profileUuid).orElse(null)
        val decryptedEmail = runCatching { encryptionConfig.decryptEmail(user.email) }.getOrElse { "[encrypted]" }
        return DataExportResponse(
            userId = user.id,
            email = decryptedEmail,
            role = user.role.name,
            allergenProfile = profile?.let {
                AllergenProfileExport(allergenCodes = it.allergenCodes, severityNotes = it.severityNotes)
            }
        )
    }

    override fun deleteUserData(userId: UUID) {
        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        userAllergenProfileRepository.deleteByProfileUuid(user.profileUuid)
        consentRecordRepository.revokeAllByProfileUuid(user.profileUuid, OffsetDateTime.now())
        oAuthAccountRepository.deleteByUserId(userId)
        val anonymizedHash = "deleted_${UUID.randomUUID()}"
        val anonymizedEmail = anonymizedHash.toByteArray()
        val anonymized = UserAccount(
            id = user.id,
            email = anonymizedEmail,
            emailHash = anonymizedHash,
            passwordHash = null,
            profileUuid = user.profileUuid,
            role = user.role,
            isActive = false,
            createdAt = user.createdAt
        )
        userAccountRepository.save(anonymized)
    }
}
