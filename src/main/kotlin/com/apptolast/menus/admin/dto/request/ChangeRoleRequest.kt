package com.apptolast.menus.admin.dto.request

import com.apptolast.menus.consumer.model.enum.UserRole
import jakarta.validation.constraints.NotNull

data class ChangeRoleRequest(
    @field:NotNull val role: UserRole
)
