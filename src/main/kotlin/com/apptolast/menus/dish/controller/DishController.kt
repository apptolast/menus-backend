package com.apptolast.menus.dish.controller

import com.apptolast.menus.auth.service.ConsentService
import com.apptolast.menus.consumer.service.UserAllergenProfileService
import com.apptolast.menus.dish.dto.response.DishResponse
import com.apptolast.menus.dish.service.DishService
import com.apptolast.menus.menu.repository.MenuSectionRepository
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.apptolast.menus.shared.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/sections/{sectionId}/dishes")
@Tag(name = "Dishes (Public)", description = "Dish listing with allergen semáforo")
class DishController(
    private val dishService: DishService,
    private val userAllergenProfileService: UserAllergenProfileService,
    private val consentService: ConsentService,
    private val menuSectionRepository: MenuSectionRepository
) {

    @GetMapping
    @Operation(summary = "List dishes in a section with allergen semáforo if authenticated with consent")
    fun getDishesBySection(
        @PathVariable restaurantId: UUID,
        @PathVariable sectionId: UUID,
        @AuthenticationPrincipal principal: UserPrincipal?
    ): ResponseEntity<List<DishResponse>> {
        if (!menuSectionRepository.existsByIdAndMenuRestaurantId(sectionId, restaurantId)) {
            throw ResourceNotFoundException("SECTION_NOT_FOUND", "Section not found for this restaurant")
        }
        val userAllergenCodes = principal
            ?.takeIf { consentService.hasActiveConsent(it.profileUuid) }
            ?.let { p ->
                runCatching { userAllergenProfileService.getProfile(p.profileUuid).allergenCodes }
                    .getOrElse { emptyList() }
            }
        return ResponseEntity.ok(dishService.findBySectionWithFilter(sectionId, userAllergenCodes))
    }
}
