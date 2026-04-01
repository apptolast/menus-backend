package com.apptolast.menus.auth.service

import com.apptolast.menus.auth.dto.response.WhitelistResponse

interface AdminWhitelistService {
    fun findAll(): List<WhitelistResponse>
    fun addEmail(email: String): WhitelistResponse
    fun removeEmail(email: String)
    fun isWhitelisted(email: String): Boolean
}
