package com.apptolast.menus.menudigitalcard.service

import com.apptolast.menus.menudigitalcard.dto.response.MenuDigitalCardResponse
import java.util.UUID

interface MenuDigitalCardService {
    fun create(menuId: UUID, dishId: UUID): MenuDigitalCardResponse
    fun findByMenuId(menuId: UUID): List<MenuDigitalCardResponse>
    fun update(id: UUID, dishId: UUID): MenuDigitalCardResponse
    fun delete(id: UUID)
}
