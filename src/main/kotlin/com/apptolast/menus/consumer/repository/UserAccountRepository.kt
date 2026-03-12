package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun findByEmail(email: String): Optional<UserAccount>
    fun existsByEmail(email: String): Boolean
}
