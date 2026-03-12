package com.apptolast.menus.digitalcard.mapper

import com.apptolast.menus.digitalcard.dto.request.CreateDigitalCardRequest
import com.apptolast.menus.digitalcard.dto.response.DigitalCardResponse
import com.apptolast.menus.digitalcard.dto.response.PublicCardResponse
import com.apptolast.menus.digitalcard.dto.response.PublicDishResponse
import com.apptolast.menus.digitalcard.dto.response.PublicSectionResponse
import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import com.apptolast.menus.dish.model.entity.Dish
import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.menu.model.entity.Menu
import com.apptolast.menus.menu.model.entity.MenuSection
import com.apptolast.menus.restaurant.model.entity.Restaurant
import java.util.UUID

fun DigitalCard.toResponse(): DigitalCardResponse = DigitalCardResponse(
    id = id,
    menuId = menuId,
    slug = slug,
    publicUrl = "/api/v1/public/cards/$slug",
    qrCodeUrl = qrCodeUrl,
    isActive = isActive,
    customCss = customCss,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CreateDigitalCardRequest.toEntity(tenantId: UUID, restaurantId: UUID): DigitalCard = DigitalCard(
    restaurantId = restaurantId,
    menuId = menuId,
    tenantId = tenantId,
    slug = slug
)

fun buildPublicCardResponse(
    restaurant: Restaurant,
    menu: Menu,
    sections: List<MenuSection>,
    dishesBySection: Map<UUID, List<Dish>>
): PublicCardResponse = PublicCardResponse(
    restaurantName = restaurant.name,
    menuName = menu.name,
    sections = sections
        .sortedBy { it.displayOrder }
        .map { section ->
            PublicSectionResponse(
                name = section.name,
                dishes = (dishesBySection[section.id] ?: emptyList())
                    .filter { it.isAvailable }
                    .map { it.toPublicDishResponse() }
            )
        }
)

fun Dish.toPublicDishResponse(): PublicDishResponse {
    val allergenCodes = allergens
        .filter { it.containmentLevel == ContainmentLevel.CONTAINS }
        .map { it.allergen.code }
    val traceCodes = allergens
        .filter { it.containmentLevel == ContainmentLevel.MAY_CONTAIN }
        .map { it.allergen.code }
    return PublicDishResponse(
        id = id,
        name = name,
        description = description,
        price = price,
        allergens = allergenCodes,
        traces = traceCodes
    )
}

fun filterDishesByExcludedAllergens(
    dishes: List<PublicDishResponse>,
    excludedAllergenCodes: Set<String>
): List<PublicDishResponse> {
    if (excludedAllergenCodes.isEmpty()) return dishes
    return dishes.filter { dish ->
        val dishAllergenCodes = (dish.allergens + dish.traces).toSet()
        dishAllergenCodes.none { it in excludedAllergenCodes }
    }
}
