package com.apptolast.menus.menudigitalcard.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateMenuDigitalCardRequest(
    @field:NotNull(message = "Menu ID is required")
    val menuId: UUID,

    @field:NotNull(message = "Dish ID is required")
    val dishId: UUID
)
