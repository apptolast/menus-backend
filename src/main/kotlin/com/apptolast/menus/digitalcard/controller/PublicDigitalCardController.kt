package com.apptolast.menus.digitalcard.controller

import com.apptolast.menus.digitalcard.dto.response.PublicCardResponse
import com.apptolast.menus.digitalcard.dto.response.PublicDishResponse
import com.apptolast.menus.digitalcard.mapper.buildPublicCardResponse
import com.apptolast.menus.digitalcard.mapper.filterDishesByExcludedAllergens
import com.apptolast.menus.digitalcard.mapper.toPublicDishResponse
import com.apptolast.menus.digitalcard.service.DigitalCardService
import com.apptolast.menus.dish.repository.DishRepository
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.dto.ErrorResponse
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/public/cards")
@Tag(name = "Public - Digital Cards", description = "Public digital card view for consumers")
class PublicDigitalCardController(
    private val digitalCardService: DigitalCardService,
    private val menuRepository: MenuRepository,
    private val restaurantRepository: RestaurantRepository,
    private val dishRepository: DishRepository
) {

    @GetMapping("/{slug}")
    @Operation(
        summary = "View a public digital card",
        description = "Returns the full menu with sections, dishes, and allergen information. " +
            "The card must be active and the menu must not be archived."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Public card data"),
        ApiResponse(
            responseCode = "404", description = "Card not found or inactive",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getPublicCard(
        @PathVariable slug: String
    ): ResponseEntity<PublicCardResponse> {
        val card = digitalCardService.findBySlug(slug)

        if (!card.isActive) {
            throw ResourceNotFoundException(
                errorCode = "DIGITAL_CARD_NOT_FOUND",
                message = "Digital card with slug '$slug' is not available"
            )
        }

        val menu = menuRepository.findById(card.menuId)
            .orElseThrow {
                ResourceNotFoundException(
                    errorCode = "MENU_NOT_FOUND",
                    message = "Menu associated with this card was not found"
                )
            }

        if (menu.isArchived) {
            throw ResourceNotFoundException(
                errorCode = "MENU_NOT_AVAILABLE",
                message = "The menu associated with this card is no longer available"
            )
        }

        val restaurant = restaurantRepository.findById(card.restaurantId)
            .orElseThrow {
                ResourceNotFoundException(
                    errorCode = "RESTAURANT_NOT_FOUND",
                    message = "Restaurant not found"
                )
            }

        val sections = menu.sections.sortedBy { it.displayOrder }
        val dishesBySection = sections.associate { section ->
            section.id to dishRepository.findWithAllergensBySectionId(section.id)
        }

        val response = buildPublicCardResponse(restaurant, menu, sections, dishesBySection)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{slug}/dishes")
    @Operation(
        summary = "Get filtered dishes from a public digital card",
        description = "Returns dishes from the card's menu, excluding dishes that contain " +
            "or may contain the specified allergens. Useful for consumer allergen filtering."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Filtered list of dishes"),
        ApiResponse(
            responseCode = "404", description = "Card not found or inactive",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    fun getFilteredDishes(
        @PathVariable slug: String,
        @Parameter(
            description = "Comma-separated allergen codes to exclude (e.g., GLUTEN,MILK)",
            example = "GLUTEN,MILK"
        )
        @RequestParam(required = false) exclude: String?
    ): ResponseEntity<List<PublicDishResponse>> {
        val card = digitalCardService.findBySlug(slug)

        if (!card.isActive) {
            throw ResourceNotFoundException(
                errorCode = "DIGITAL_CARD_NOT_FOUND",
                message = "Digital card with slug '$slug' is not available"
            )
        }

        val menu = menuRepository.findById(card.menuId)
            .orElseThrow {
                ResourceNotFoundException(
                    errorCode = "MENU_NOT_FOUND",
                    message = "Menu associated with this card was not found"
                )
            }

        if (menu.isArchived) {
            throw ResourceNotFoundException(
                errorCode = "MENU_NOT_AVAILABLE",
                message = "The menu associated with this card is no longer available"
            )
        }

        val sections = menu.sections.sortedBy { it.displayOrder }
        val allDishes = sections.flatMap { section ->
            dishRepository.findWithAllergensBySectionId(section.id)
                .filter { it.isAvailable }
                .map { it.toPublicDishResponse() }
        }

        val excludedCodes = exclude
            ?.split(",")
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

        val filteredDishes = filterDishesByExcludedAllergens(allDishes, excludedCodes)
        return ResponseEntity.ok(filteredDishes)
    }
}
