package com.apptolast.menus.consumer.service

import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import java.util.UUID

interface UserAllergenProfileService {
    fun getProfile(profileUuid: UUID): AllergenProfileResponse
    fun upsertProfile(profileUuid: UUID, request: AllergenProfileRequest): AllergenProfileResponse
}
