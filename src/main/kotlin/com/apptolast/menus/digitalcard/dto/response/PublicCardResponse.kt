package com.apptolast.menus.digitalcard.dto.response

import java.math.BigDecimal
import java.util.UUID

data class PublicCardResponse(
    val restaurantName: String,
    val menuName: String,
    val sections: List<PublicSectionResponse>
)

data class PublicSectionResponse(
    val name: String,
    val dishes: List<PublicDishResponse>
)

data class PublicDishResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val price: BigDecimal?,
    val allergens: List<String>,
    val traces: List<String>
)
