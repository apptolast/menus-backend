package com.apptolast.menus.consumer.service.impl

import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import com.apptolast.menus.consumer.model.entity.UserAllergenProfile
import com.apptolast.menus.consumer.repository.UserAllergenProfileRepository
import com.apptolast.menus.consumer.service.UserAllergenProfileService
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class UserAllergenProfileServiceImpl(
    private val userAllergenProfileRepository: UserAllergenProfileRepository
) : UserAllergenProfileService {

    @Transactional(readOnly = true)
    override fun getProfile(profileUuid: UUID): AllergenProfileResponse {
        val profile = userAllergenProfileRepository.findByProfileUuid(profileUuid)
            .orElseThrow { ResourceNotFoundException("PROFILE_NOT_FOUND", "Allergen profile not found") }
        return profile.toResponse()
    }

    override fun upsertProfile(profileUuid: UUID, request: AllergenProfileRequest): AllergenProfileResponse {
        val profile = userAllergenProfileRepository.findByProfileUuid(profileUuid)
            .map { existing ->
                existing.allergenCodes = request.allergenCodes
                existing.severityNotes = request.severityNotes
                existing.updatedAt = OffsetDateTime.now()
                existing
            }
            .orElseGet {
                UserAllergenProfile(
                    profileUuid = profileUuid,
                    allergenCodes = request.allergenCodes,
                    severityNotes = request.severityNotes
                )
            }
        return userAllergenProfileRepository.save(profile).toResponse()
    }

    override fun deleteProfile(profileUuid: UUID) {
        userAllergenProfileRepository.deleteByProfileUuid(profileUuid)
    }

    private fun UserAllergenProfile.toResponse() = AllergenProfileResponse(
        profileUuid = profileUuid,
        allergenCodes = allergenCodes,
        severityNotes = severityNotes ?: "",
        updatedAt = updatedAt
    )
}
