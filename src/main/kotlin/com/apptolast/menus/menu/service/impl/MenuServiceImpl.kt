package com.apptolast.menus.menu.service.impl

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class MenuServiceImpl(
    private val menuRepository: MenuRepository,
    private val menuSectionRepository: MenuSectionRepository
) : MenuService {

    @Transactional(readOnly = true)
    override fun findByRestaurant(restaurantId: UUID, includeArchived: Boolean): List<MenuResponse> {
        val menus = if (includeArchived)
            menuRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)
        else
            menuRepository.findByRestaurantIdAndIsArchivedFalseOrderByDisplayOrderAsc(restaurantId)
        return menus.map { it.toResponse() }
    }

    override fun create(restaurantId: UUID, tenantId: UUID, request: MenuRequest): MenuResponse {
        val menu = Menu(
            restaurantId = restaurantId,
            tenantId = tenantId,
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder
        )
        return menuRepository.save(menu).toResponse()
    }

    override fun update(id: UUID, tenantId: UUID, request: MenuRequest): MenuResponse {
        val menu = findMenuForTenantOrThrow(id, tenantId)
        menu.name = request.name
        menu.description = request.description
        menu.displayOrder = request.displayOrder
        menu.updatedAt = OffsetDateTime.now()
        return menuRepository.save(menu).toResponse()
    }

    override fun archive(id: UUID, tenantId: UUID) {
        val menu = findMenuForTenantOrThrow(id, tenantId)
        menu.isArchived = true
        menu.updatedAt = OffsetDateTime.now()
        menuRepository.save(menu)
    }

    override fun addSection(menuId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse {
        val menu = findMenuOrThrow(menuId)
        val section = MenuSection(
            menu = menu,
            tenantId = tenantId,
            name = request.name,
            displayOrder = request.displayOrder
        )
        return menuSectionRepository.save(section).toResponse()
    }

    override fun updateSection(sectionId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse {
        val section = menuSectionRepository.findByIdAndTenantId(sectionId, tenantId)
            .orElseThrow { ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found") }
        section.name = request.name
        section.displayOrder = request.displayOrder
        return menuSectionRepository.save(section).toResponse()
    }

    override fun deleteSection(sectionId: UUID, tenantId: UUID) {
        if (!menuSectionRepository.existsByIdAndTenantId(sectionId, tenantId)) {
            throw ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found")
        }
        menuSectionRepository.deleteById(sectionId)
    }

    private fun findMenuOrThrow(id: UUID): Menu =
        menuRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found") }

    private fun findMenuForTenantOrThrow(id: UUID, tenantId: UUID): Menu =
        menuRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found") }

    private fun Menu.toResponse() = MenuResponse(
        id = id,
        name = name,
        description = description,
        isArchived = isArchived,
        displayOrder = displayOrder,
        sections = sections.map { it.toResponse() },
        updatedAt = updatedAt
    )

    private fun MenuSection.toResponse() = SectionResponse(
        id = id,
        name = name,
        displayOrder = displayOrder
    )
}
