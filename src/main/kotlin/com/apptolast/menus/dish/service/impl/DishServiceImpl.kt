package com.apptolast.menus.dish.service.impl

import com.apptolast.menus.allergen.repository.AllergenRepository
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
                price = request.price,
                imageUrl = request.imageUrl,
                available = request.available,
                displayOrder = request.displayOrder
            )
        )
        request.allergens.forEach { allergenReq ->
            saveAllergen(dish, allergenReq)
        }
        entityManager.flush()
        entityManager.clear()
        return dishRepository.findByIdWithAllergens(dish.id).get().toResponse(null)
    }

    override fun update(id: UUID, request: DishRequest): DishResponse {
        val dish = findDishOrThrow(id)
        dish.name = request.name
        dish.description = request.description
        dish.price = request.price
        dish.imageUrl = request.imageUrl
        dish.available = request.available
        dish.displayOrder = request.displayOrder
        dish.updatedAt = OffsetDateTime.now()
        dishRepository.save(dish)

        dishAllergenRepository.deleteByDishId(id)
        request.allergens.forEach { allergenReq ->
            saveAllergen(dish, allergenReq)
        }

        entityManager.flush()
        entityManager.clear()
        return dishRepository.findByIdWithAllergens(dish.id).get().toResponse(null)
    }

    override fun delete(id: UUID) {
        if (!dishRepository.existsById(id)) {
            throw ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found")
        }
        dishRepository.deleteById(id)
    }

    override fun addAllergen(
        dishId: UUID,
        request: DishAllergenRequest
    ): DishResponse {
        val dish = findDishOrThrow(dishId)
        val allergen = allergenRepository.findByCode(request.allergenCode)
            ?: throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${request.allergenCode} not found")
        val containmentLevel = ContainmentLevel.valueOf(request.containmentLevel)

        val dishAllergen = DishAllergen(
            dish = dish,
            allergen = allergen,
            containmentLevel = containmentLevel,
            notes = request.notes
        )
        dishAllergenRepository.save(dishAllergen)
        entityManager.flush()
        entityManager.clear()
        return dishRepository.findByIdWithAllergens(dishId).get().toResponse(null)
    }

    override fun removeAllergen(dishId: UUID, allergenId: Int) {
        dishAllergenRepository.deleteByDishIdAndAllergenId(dishId, allergenId)
    }

    private fun findDishOrThrow(id: UUID): Dish =
        dishRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("DISH_NOT_FOUND", "Dish not found") }

    private fun saveAllergen(dish: Dish, req: DishAllergenRequest) {
        val allergen = allergenRepository.findByCode(req.allergenCode)
            ?: throw ResourceNotFoundException("ALLERGEN_NOT_FOUND", "Allergen ${req.allergenCode} not found")
        dishAllergenRepository.save(
            DishAllergen(
                dish = dish,
                allergen = allergen,
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
            description = description ?: "",
            price = price,
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
