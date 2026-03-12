package com.apptolast.menus.dish.service

import com.apptolast.menus.dish.dto.request.DishRequest
import com.apptolast.menus.dish.dto.request.DishAllergenRequest
import com.apptolast.menus.dish.dto.response.DishResponse
import java.util.UUID

interface DishService {
    fun findBySectionWithFilter(sectionId: UUID, userAllergenCodes: List<String>?): List<DishResponse>
    fun findByRestaurant(restaurantId: UUID): List<DishResponse>
    fun create(request: DishRequest): DishResponse
    fun update(id: UUID, request: DishRequest): DishResponse
    fun delete(id: UUID)
    fun addAllergen(dishId: UUID, request: DishAllergenRequest): DishResponse
    fun removeAllergen(dishId: UUID, allergenId: Int)
}
