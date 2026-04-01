package com.apptolast.menus.menudigitalcard.mapper

import com.apptolast.menus.menudigitalcard.dto.response.MenuDigitalCardResponse
import com.apptolast.menus.menudigitalcard.model.entity.MenuDigitalCard

fun MenuDigitalCard.toResponse(): MenuDigitalCardResponse =
    MenuDigitalCardResponse(
        id = id,
        menuId = menu.id,
        dishId = dish.id,
        dishName = dish.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
