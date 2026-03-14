package com.apptolast.menus.menu.service.impl

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.MenuRecipeResponse
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuRecipe
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.menu.repository.MenuRecipeRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.menu.service.MenuService
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class MenuServiceImpl(
    private val menuRepository: MenuRepository,
    private val menuSectionRepository: MenuSectionRepository,
    private val recipeRepository: RecipeRepository,
    private val menuRecipeRepository: MenuRecipeRepository
) : MenuService {

    private val logger = LoggerFactory.getLogger(MenuServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByRestaurant(restaurantId: UUID, includeArchived: Boolean): List<MenuResponse> {
        val menus = if (includeArchived)
            menuRepository.findByRestaurantIdWithSections(restaurantId)
        else
            menuRepository.findByRestaurantIdAndArchivedFalseWithSections(restaurantId)
        return menus.map { it.toResponse() }
    }

    override fun create(restaurantId: UUID, request: MenuRequest): MenuResponse {
        logger.info("Creating menu '{}' for restaurant {}", request.name, restaurantId)
        val menu = Menu(
            restaurantId = restaurantId,
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder,
            restaurantLogoUrl = request.restaurantLogoUrl,
            companyLogoUrl = request.companyLogoUrl
        )
        val saved = menuRepository.save(menu)
        if (!request.recipeIds.isNullOrEmpty()) {
            syncRecipes(saved, request.recipeIds)
            menuRepository.save(saved)
        }
        return saved.toResponse()
    }

    override fun update(id: UUID, request: MenuRequest): MenuResponse {
        logger.info("Updating menu {}", id)
        val menu = findMenuOrThrow(id)
        menu.name = request.name
        menu.description = request.description
        menu.displayOrder = request.displayOrder
        menu.restaurantLogoUrl = request.restaurantLogoUrl
        menu.companyLogoUrl = request.companyLogoUrl
        menu.updatedAt = OffsetDateTime.now()
        if (request.recipeIds != null) {
            syncRecipes(menu, request.recipeIds)
        }
        return menuRepository.save(menu).toResponse()
    }

    override fun archive(id: UUID) {
        val menu = findMenuOrThrow(id)
        menu.archived = true
        menu.published = false
        menu.updatedAt = OffsetDateTime.now()
        menuRepository.save(menu)
    }

    override fun addSection(menuId: UUID, request: SectionRequest): SectionResponse {
        val menu = findMenuOrThrow(menuId)
        val section = MenuSection(
            menu = menu,
            name = request.name,
            displayOrder = request.displayOrder
        )
        return menuSectionRepository.save(section).toResponse()
    }

    override fun updateSection(sectionId: UUID, request: SectionRequest): SectionResponse {
        val section = menuSectionRepository.findById(sectionId)
            .orElseThrow { ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found") }
        section.name = request.name
        section.displayOrder = request.displayOrder
        return menuSectionRepository.save(section).toResponse()
    }

    override fun deleteSection(sectionId: UUID) {
        if (!menuSectionRepository.existsById(sectionId)) {
            throw ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found")
        }
        menuSectionRepository.deleteById(sectionId)
    }

    override fun publish(menuId: UUID, published: Boolean): MenuResponse {
        val menu = findMenuOrThrow(menuId)
        menu.published = published
        if (published) {
            menu.archived = false
        }
        menu.updatedAt = OffsetDateTime.now()
        return menuRepository.save(menu).toResponse()
    }

    override fun updateRecipes(menuId: UUID, recipeIds: List<UUID>): MenuResponse {
        logger.info("Updating recipes for menu {}: {} recipe(s)", menuId, recipeIds.size)
        val menu = findMenuOrThrow(menuId)
        syncRecipes(menu, recipeIds)
        menu.updatedAt = OffsetDateTime.now()
        return menuRepository.save(menu).toResponse()
    }

    private fun syncRecipes(menu: Menu, recipeIds: List<UUID>) {
        val recipes = recipeRepository.findAllById(recipeIds)
        if (recipes.size != recipeIds.size) {
            val found = recipes.map { it.id }.toSet()
            val missing = recipeIds.filter { it !in found }
            throw ResourceNotFoundException("RECIPE_NOT_FOUND", "Recipes not found: $missing")
        }
        menu.menuRecipes.clear()
        recipes.forEach { recipe ->
            menu.menuRecipes.add(MenuRecipe(menu = menu, recipe = recipe))
        }
    }

    private fun findMenuOrThrow(id: UUID): Menu =
        menuRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("MENU_NOT_FOUND", "Menu not found") }

    private fun Menu.toResponse() = MenuResponse(
        id = id,
        name = name,
        description = description ?: "",
        published = published,
        archived = archived,
        displayOrder = displayOrder,
        sections = sections.map { it.toResponse() },
        restaurantLogoUrl = restaurantLogoUrl,
        companyLogoUrl = companyLogoUrl,
        recipes = menuRecipes.map { MenuRecipeResponse(id = it.recipe.id, name = it.recipe.name) },
        updatedAt = updatedAt
    )

    private fun MenuSection.toResponse() = SectionResponse(
        id = id,
        name = name,
        displayOrder = displayOrder
    )
}
