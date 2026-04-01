package com.apptolast.menus.menudigitalcard.service.impl

import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menudigitalcard.dto.response.MenuDigitalCardResponse
import com.apptolast.menus.menudigitalcard.mapper.toResponse
import com.apptolast.menus.menudigitalcard.model.entity.MenuDigitalCard
import com.apptolast.menus.menudigitalcard.repository.MenuDigitalCardRepository
import com.apptolast.menus.menudigitalcard.service.MenuDigitalCardService
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class MenuDigitalCardServiceImpl(
    private val menuDigitalCardRepository: MenuDigitalCardRepository,
    private val menuRepository: MenuRepository,
    private val dishRepository: DishRepository
) : MenuDigitalCardService {

    override fun create(menuId: UUID, dishId: UUID): MenuDigitalCardResponse {
        val menu = menuRepository.findById(menuId)
            .orElseThrow { ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found") }
        val dish = dishRepository.findById(dishId)
            .orElseThrow { ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found") }

        if (menuDigitalCardRepository.existsByMenuIdAndDishId(menuId, dishId)) {
            throw ConflictException("MENU_DIGITAL_CARD_DUPLICATE", "This dish is already assigned to this menu's digital card")
        }

        val entity = MenuDigitalCard(menu = menu, dish = dish)
        return menuDigitalCardRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    override fun findByMenuId(menuId: UUID): List<MenuDigitalCardResponse> {
        if (!menuRepository.existsById(menuId)) {
            throw ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found")
        }
        return menuDigitalCardRepository.findByMenuId(menuId).map { it.toResponse() }
    }

    override fun update(id: UUID, dishId: UUID): MenuDigitalCardResponse {
        val existing = menuDigitalCardRepository.findById(id)
            ?: throw ResourceNotFoundException("MENU_DIGITAL_CARD_NOT_FOUND", "Menu digital card assignment not found")

        val dish = dishRepository.findById(dishId)
            .orElseThrow { ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found") }

        if (menuDigitalCardRepository.existsByMenuIdAndDishId(existing.menu.id, dishId)) {
            throw ConflictException("MENU_DIGITAL_CARD_DUPLICATE", "This dish is already assigned to this menu's digital card")
        }

        existing.dish = dish
        existing.updatedAt = OffsetDateTime.now()
        return menuDigitalCardRepository.save(existing).toResponse()
    }

    override fun delete(id: UUID) {
        if (!menuDigitalCardRepository.existsById(id)) {
            throw ResourceNotFoundException("MENU_DIGITAL_CARD_NOT_FOUND", "Menu digital card assignment not found")
        }
        menuDigitalCardRepository.deleteById(id)
    }
}
