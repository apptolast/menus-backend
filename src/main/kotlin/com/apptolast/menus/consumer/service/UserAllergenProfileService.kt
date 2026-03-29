package com.apptolast.menus.consumer.service

import com.apptolast.menus.consumer.dto.request.AllergenProfileRequest
import com.apptolast.menus.consumer.dto.response.AllergenProfileResponse
import java.util.UUID

interface UserAllergenProfileService {
    fun getProfile(userId: UUID): AllergenProfileResponse
    fun upsertProfile(userId: UUID, request: AllergenProfileRequest): AllergenProfileResponse
    fun deleteProfile(userId: UUID)
}
