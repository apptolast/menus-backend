package com.apptolast.menus.menudigitalcard.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class UpdateMenuDigitalCardRequest(
    @field:NotNull(message = "Dish ID is required")
    val dishId: UUID
)
