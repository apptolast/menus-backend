package com.apptolast.menus.dish.service.impl

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.audit.model.entity.AllergenAuditLog
import com.apptolast.menus.audit.model.entity.AuditAction
import com.apptolast.menus.audit.repository.AllergenAuditLogRepository
import com.apptolast.menus.dish.dto.request.DishAllergenRequest
import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.response.DishAllergenResponse
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.model.enum.SafetyLevel
import com.apptolast.menus.dish.repository.DishAllergenRepository
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.dish.service.AllergenFilterService
import com.apptolast.menus.dish.service.DishService
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional
class DishServiceImpl(
    private val dishRepository: DishRepository,
    private val dishAllergenRepository: DishAllergenRepository,
    private val allergenRepository: AllergenRepository,
    private val allergenAuditLogRepository: AllergenAuditLogRepository,
    private val menuSectionRepository: MenuSectionRepository,
    private val allergenFilterService: AllergenFilterService
) : DishService {

    @Transactional(readOnly = true)
    override fun findBySectionWithFilter(sectionId: UUID, userAllergenCodes: List<String>?): List<DishResponse> =
        dishRepository.findWithAllergensBySectionId(sectionId).map { dish ->
            dish.toResponse(userAllergenCodes)
        }

    @Transactional(readOnly = true)
    override fun findByRestaurant(restaurantId: UUID): List<DishResponse> =
        dishRepository.findBySectionMenuRestaurantId(restaurantId).map { it.toResponse(null) }

    override fun create(tenantId: UUID, request: DishRequest): DishResponse {
        val section = menuSectionRepository.findById(request.sectionId)
            .orElseThrow { ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found") }
        val dish = dishRepository.save(
            Dish(
                section = section,
                tenantId = tenantId,
                name = request.name,
                description = request.description,
                price = request.price,
                imageUrl = request.imageUrl
            )
        )
        request.allergens.forEach { allergenReq ->
            saveAllergen(dish, allergenReq, tenantId)
        }
        return dishRepository.findById(dish.id).get().toResponse(null)
    }

    override fun update(id: UUID, tenantId: UUID, request: DishRequest): DishResponse {
        val dish = findDishOrThrow(id)
        dish.name = request.name
        dish.description = request.description
        dish.price = request.price
        dish.imageUrl = request.imageUrl
        dish.updatedAt = OffsetDateTime.now()
        dishRepository.save(dish)
        return dish.toResponse(null)
    }

    override fun delete(id: UUID, tenantId: UUID) {
        if (!dishRepository.existsById(id)) {
            throw ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found")
        }
        dishRepository.deleteById(id)
    }

    override fun addAllergen(
        dishId: UUID,
        tenantId: UUID,
        changedByProfileUuid: UUID,
        request: DishAllergenRequest
    ): DishResponse {
        val dish = findDishOrThrow(dishId)
        val allergen = allergenRepository.findByCode(request.allergenCode)
            .orElseThrow { ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${request.allergenCode} not found") }
        val containmentLevel = ContainmentLevel.valueOf(request.containmentLevel)

        val existing = dishAllergenRepository.findByDishIdAndAllergenId(dishId, allergen.id)
        val oldLevel = existing.map { it.containmentLevel }.orElse(null)

        val dishAllergen = existing.map { da ->
            da.containmentLevel = containmentLevel
            da.notes = request.notes
            da
        }.orElseGet {
            DishAllergen(
                dish = dish,
                allergen = allergen,
                tenantId = tenantId,
                containmentLevel = containmentLevel,
                notes = request.notes
            )
        }
        dishAllergenRepository.save(dishAllergen)

        allergenAuditLogRepository.save(
            AllergenAuditLog(
                dishId = dishId,
                allergenId = allergen.id,
                tenantId = tenantId,
                changedByUuid = changedByProfileUuid,
                action = if (oldLevel == null) AuditAction.ADD else AuditAction.UPDATE,
                oldLevel = oldLevel,
                newLevel = containmentLevel
            )
        )
        return findDishOrThrow(dishId).toResponse(null)
    }

    override fun removeAllergen(dishId: UUID, allergenId: Int, tenantId: UUID, changedByProfileUuid: UUID) {
        val existing = dishAllergenRepository.findByDishIdAndAllergenId(dishId, allergenId)
            .orElseThrow { ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen not on this dish") }
        allergenAuditLogRepository.save(
            AllergenAuditLog(
                dishId = dishId,
                allergenId = allergenId,
                tenantId = tenantId,
                changedByUuid = changedByProfileUuid,
                action = AuditAction.REMOVE,
                oldLevel = existing.containmentLevel,
                newLevel = null
            )
        )
        dishAllergenRepository.deleteByDishIdAndAllergenId(dishId, allergenId)
    }

    private fun findDishOrThrow(id: UUID): Dish =
        dishRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found") }

    private fun saveAllergen(dish: Dish, req: DishAllergenRequest, tenantId: UUID) {
        val allergen = allergenRepository.findByCode(req.allergenCode)
            .orElseThrow { ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${req.allergenCode} not found") }
        dishAllergenRepository.save(
            DishAllergen(
                dish = dish,
                allergen = allergen,
                tenantId = tenantId,
                containmentLevel = ContainmentLevel.valueOf(req.containmentLevel),
                notes = req.notes
            )
        )
    }

    private fun Dish.toResponse(userAllergenCodes: List<String>?): DishResponse {
        val safetyLevel = userAllergenCodes?.let {
            allergenFilterService.computeSafetyLevel(allergens, it)
        }
        val matchedAllergens = userAllergenCodes?.let {
            allergenFilterService.getMatchedAllergens(allergens, it)
        } ?: emptyList()
        return DishResponse(
            id = id,
            name = name,
            description = description,
            price = price,
            imageUrl = imageUrl,
            isAvailable = isAvailable,
            safetyLevel = safetyLevel,
            matchedAllergens = matchedAllergens,
            allergens = allergens.map { da ->
                DishAllergenResponse(
                    allergenId = da.allergen.id,
                    code = da.allergen.code,
                    name = da.allergen.translations.firstOrNull { t -> t.locale == "es" }?.name ?: da.allergen.code,
                    containmentLevel = da.containmentLevel.name,
                    notes = da.notes
                )
            },
            updatedAt = updatedAt
        )
    }
}
