package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.UserAllergenProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserAllergenProfileRepository : JpaRepository<UserAllergenProfile, UUID> {
    fun findByProfileUuid(profileUuid: UUID): Optional<UserAllergenProfile>
    fun deleteByProfileUuid(profileUuid: UUID)
}
