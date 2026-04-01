package com.apptolast.menus.dish.service.impl

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.dish.dto.DishAllergenData
import com.apptolast.menus.dish.dto.request.DishAllergenRequest
import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.response.DishAllergenResponse
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.repository.DishAllergenRepository
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.dish.service.AllergenFilterService
import com.apptolast.menus.dish.service.DishService
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import jakarta.persistence.EntityManager
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
    private val menuSectionRepository: MenuSectionRepository,
    private val allergenFilterService: AllergenFilterService,
    private val entityManager: EntityManager
) : DishService {

    @Transactional(readOnly = true)
    override fun findBySectionWithFilter(sectionId: UUID, userAllergenCodes: List<String>?): List<DishResponse> =
        dishRepository.findBySectionIdWithAllergensAndRecipe(sectionId).map { dish ->
            dish.toResponse(userAllergenCodes)
        }

    @Transactional(readOnly = true)
    override fun findByRestaurant(restaurantId: UUID): List<DishResponse> =
        dishRepository.findByRestaurantIdWithAllergens(restaurantId).map { it.toResponse(null) }

    override fun create(request: DishRequest): DishResponse {
        val section = menuSectionRepository.findById(request.sectionId)
            .orElseThrow { ResourceNotFoundException("SECTION_NOT_FOUND", "Menu section not found") }
        val dish = dishRepository.save(
            Dish(
                section = section,
                name = request.name,
                description = request.description,
                imageUrl = request.imageUrl,
                available = request.available,
                displayOrder = request.displayOrder ?: 0
            )
        )
        if (request.allergens.isNotEmpty()) {
            saveAllergensBatch(dish, request.allergens)
        }
        return flushClearAndLoad(dish.id)
    }

    override fun update(id: UUID, request: DishRequest): DishResponse {
        val dish = findDishOrThrow(id)
        dish.name = request.name
        dish.description = request.description
        dish.imageUrl = request.imageUrl
        dish.available = request.available
        dish.displayOrder = request.displayOrder ?: 0
        dish.updatedAt = OffsetDateTime.now()
        dishRepository.save(dish)

        if (request.allergens.isNotEmpty()) {
            val currentAllergens = dishAllergenRepository.findByDishId(id)
            val currentAllergenCodes = currentAllergens.map { it.allergen.code }.toSet()

            // Batch fetch all new allergen codes in one query
            val newCodes = request.allergens.map { it.allergenCode }.filter { it !in currentAllergenCodes }
            val newAllergenEntities = if (newCodes.isNotEmpty()) {
                allergenRepository.findAllByCodeIn(newCodes).associateBy { it.code }
            } else emptyMap()

            for (allergenReq in request.allergens) {
                if (allergenReq.allergenCode in currentAllergenCodes) {
                    val current = currentAllergens.first { it.allergen.code == allergenReq.allergenCode }
                    current.containmentLevel = ContainmentLevel.valueOf(allergenReq.containmentLevel)
                    current.notes = allergenReq.notes
                    dishAllergenRepository.save(current)
                } else {
                    val allergen = newAllergenEntities[allergenReq.allergenCode]
                        ?: throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${allergenReq.allergenCode} not found")
                    dishAllergenRepository.save(
                        DishAllergen(
                            dish = dish,
                            allergen = allergen,
                            containmentLevel = ContainmentLevel.valueOf(allergenReq.containmentLevel),
                            notes = allergenReq.notes
                        )
                    )
                }
            }
        }

        return flushClearAndLoad(dish.id)
    }

    override fun delete(id: UUID) {
        if (!dishRepository.existsById(id)) {
            throw ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found")
        }
        dishRepository.deleteById(id)
    }

    override fun addAllergen(dishId: UUID, request: DishAllergenRequest): DishResponse {
        val dish = findDishOrThrow(dishId)
        val allergen = allergenRepository.findByCode(request.allergenCode)
            ?: throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${request.allergenCode} not found")
        dishAllergenRepository.save(
            DishAllergen(
                dish = dish,
                allergen = allergen,
                containmentLevel = ContainmentLevel.valueOf(request.containmentLevel),
                notes = request.notes
            )
        )
        return flushClearAndLoad(dishId)
    }

    override fun removeAllergen(dishId: UUID, allergenId: Int) {
        dishAllergenRepository.deleteByDishIdAndAllergenId(dishId, allergenId)
    }

    private fun findDishOrThrow(id: UUID): Dish =
        dishRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found") }

    private fun flushClearAndLoad(id: UUID): DishResponse {
        entityManager.flush()
        entityManager.clear()
        return dishRepository.findByIdWithAllergens(id).get().toResponse(null)
    }

    private fun saveAllergensBatch(dish: Dish, allergens: List<DishAllergenRequest>) {
        val codes = allergens.map { it.allergenCode }
        val allergenEntities = allergenRepository.findAllByCodeIn(codes)
        val allergenMap = allergenEntities.associateBy { it.code }

        val missing = codes.filter { it !in allergenMap }
        if (missing.isNotEmpty()) {
            throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergens not found: $missing")
        }

        dishAllergenRepository.saveAll(allergens.map { req ->
            DishAllergen(
                dish = dish,
                allergen = allergenMap[req.allergenCode]!!,
                containmentLevel = ContainmentLevel.valueOf(req.containmentLevel),
                notes = req.notes
            )
        })
    }

    private fun Dish.toResponse(userAllergenCodes: List<String>?): DishResponse {
        val allergenData = allergens.map { DishAllergenData(it.allergen.code, it.containmentLevel) }
        val safetyLevel = userAllergenCodes?.let {
            allergenFilterService.computeSafetyLevel(allergenData, it)
        }
        val matchedAllergens = userAllergenCodes?.let {
            allergenFilterService.getMatchedAllergens(allergenData, it)
        } ?: emptyList()
        return DishResponse(
            id = id,
            name = name,
            description = description ?: "",
            price = recipe?.price,
            sectionId = section.id,
            imageUrl = imageUrl,
            available = available,
            safetyLevel = safetyLevel,
            matchedAllergens = matchedAllergens,
            allergens = allergens.map { da ->
                DishAllergenResponse(
                    allergenId = da.allergen.id,
                    code = da.allergen.code,
                    name = da.allergen.nameEs,
                    containmentLevel = da.containmentLevel.name,
                    notes = da.notes ?: ""
                )
            },
            updatedAt = updatedAt
        )
    }
}
