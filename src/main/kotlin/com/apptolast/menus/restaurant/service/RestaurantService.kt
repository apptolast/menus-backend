package com.apptolast.menus.restaurant.service

import com.apptolast.menus.restaurant.dto.request.RestaurantRequest
import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.shared.dto.PageResponse
import java.util.UUID

interface RestaurantService {
    fun findAll(name: String?, page: Int, size: Int): PageResponse<RestaurantResponse>
    fun findById(id: UUID): RestaurantResponse
    fun create(request: RestaurantRequest): RestaurantResponse
    fun update(restaurantId: UUID, request: RestaurantRequest): RestaurantResponse
    fun deactivate(restaurantId: UUID)
}
