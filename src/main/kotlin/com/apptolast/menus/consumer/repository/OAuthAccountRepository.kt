package com.apptolast.menus.consumer.repository

import com.apptolast.menus.consumer.model.entity.OAuthAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OAuthAccountRepository : JpaRepository<OAuthAccount, UUID> {
    fun findByProviderAndProviderId(provider: String, providerId: String): Optional<OAuthAccount>
    fun deleteByUserId(userId: UUID)
}
