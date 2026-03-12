package com.apptolast.menus.auth.repository

import com.apptolast.menus.auth.model.entity.AdminWhitelist
import org.springframework.data.jpa.repository.JpaRepository

interface AdminWhitelistRepository : JpaRepository<AdminWhitelist, Int> {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): AdminWhitelist?
    fun deleteByEmail(email: String)
}
