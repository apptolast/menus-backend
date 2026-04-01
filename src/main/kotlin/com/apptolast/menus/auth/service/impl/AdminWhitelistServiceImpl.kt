package com.apptolast.menus.auth.service.impl

import com.apptolast.menus.auth.dto.response.WhitelistResponse
import com.apptolast.menus.auth.model.entity.AdminWhitelist
import com.apptolast.menus.auth.repository.AdminWhitelistRepository
import com.apptolast.menus.auth.service.AdminWhitelistService
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminWhitelistServiceImpl(
    private val adminWhitelistRepository: AdminWhitelistRepository
) : AdminWhitelistService {

    @Transactional(readOnly = true)
    override fun findAll(): List<WhitelistResponse> =
        adminWhitelistRepository.findAll().map { it.toResponse() }

    @Transactional
    override fun addEmail(email: String): WhitelistResponse {
        if (adminWhitelistRepository.existsByEmail(email)) {
            throw ConflictException("EMAIL_ALREADY_WHITELISTED", "Email is already in the admin whitelist")
        }
        return adminWhitelistRepository.save(AdminWhitelist(email = email)).toResponse()
    }

    @Transactional
    override fun removeEmail(email: String) {
        adminWhitelistRepository.findByEmail(email)
            ?: throw ResourceNotFoundException("WHITELIST_ENTRY_NOT_FOUND", "Email not found in the admin whitelist")
        adminWhitelistRepository.deleteByEmail(email)
    }

    @Transactional(readOnly = true)
    override fun isWhitelisted(email: String): Boolean =
        adminWhitelistRepository.existsByEmail(email)

    private fun AdminWhitelist.toResponse() = WhitelistResponse(id = id, email = email)
}
