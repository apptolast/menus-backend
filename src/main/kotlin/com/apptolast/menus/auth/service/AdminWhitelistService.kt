package com.apptolast.menus.auth.service

import com.apptolast.menus.auth.model.entity.AdminWhitelist

interface AdminWhitelistService {
    fun findAll(): List<AdminWhitelist>
    fun addEmail(email: String): AdminWhitelist
    fun removeEmail(email: String)
    fun isWhitelisted(email: String): Boolean
}
