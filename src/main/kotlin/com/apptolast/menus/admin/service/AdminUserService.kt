package com.apptolast.menus.admin.service

import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface AdminUserService {
    fun listUsers(role: UserRole?, pageable: Pageable): Page<UserAccount>
    fun changeRole(userId: UUID, newRole: UserRole): UserAccount
}
