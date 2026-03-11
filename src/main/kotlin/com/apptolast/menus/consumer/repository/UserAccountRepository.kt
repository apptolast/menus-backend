package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun findByEmailHash(emailHash: String): Optional<UserAccount>
    fun findByProfileUuid(profileUuid: UUID): Optional<UserAccount>
    fun existsByEmailHash(emailHash: String): Boolean
    fun findByRole(role: UserRole, pageable: Pageable): Page<UserAccount>
}
