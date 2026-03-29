package com.apptolast.menus.restaurant.service.impl

import com.apptolast.menus.restaurant.dto.request.RestaurantRequest
import com.apptolast.menus.restaurant.dto.response.RestaurantResponse
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.restaurant.service.RestaurantService
import com.apptolast.menus.shared.dto.PageResponse
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RestaurantServiceImpl(
    private val restaurantRepository: RestaurantRepository
) : RestaurantService {

    override fun findAll(name: String?, page: Int, size: Int): PageResponse<RestaurantResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val result = if (name.isNullOrBlank()) {
            restaurantRepository.findByActiveTrue(pageable)
        } else {
            restaurantRepository.findActiveByNameContaining(name, pageable)
        }
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            last = result.isLast
        )
    }

    override fun findById(id: UUID): RestaurantResponse =
        restaurantRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("RESTAURANT_NOT_FOUND", "Restaurant not found") }
            .toResponse()

    @Transactional
    override fun create(request: RestaurantRequest): RestaurantResponse {
        val restaurant = Restaurant(
            name = request.name,
            slug = request.slug,
            description = request.description,
            address = request.address,
            phone = request.phone,
            logoUrl = request.logoUrl
        )
        return restaurantRepository.save(restaurant).toResponse()
    }

    @Transactional
    override fun update(restaurantId: UUID, request: RestaurantRequest): RestaurantResponse {
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("RESTAURANT_NOT_FOUND", "Restaurant not found") }
        restaurant.name = request.name
        restaurant.slug = request.slug
        restaurant.description = request.description
        restaurant.address = request.address
        restaurant.phone = request.phone
        restaurant.logoUrl = request.logoUrl
        restaurant.updatedAt = OffsetDateTime.now()
        return restaurantRepository.save(restaurant).toResponse()
    }

    @Transactional
    override fun deactivate(restaurantId: UUID) {
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("RESTAURANT_NOT_FOUND", "Restaurant not found") }
        restaurant.active = false
        restaurant.updatedAt = OffsetDateTime.now()
        restaurantRepository.save(restaurant)
    }

    private fun Restaurant.toResponse() = RestaurantResponse(
        id = id,
        name = name,
        slug = slug,
        description = description ?: "",
        address = address ?: "",
        phone = phone ?: "",
        logoUrl = logoUrl,
        active = active,
        createdAt = createdAt
    )
}
