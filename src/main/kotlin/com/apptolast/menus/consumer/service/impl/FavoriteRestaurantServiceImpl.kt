package com.apptolast.menus.consumer.service.impl

import com.apptolast.menus.consumer.dto.response.FavoriteRestaurantResponse
import com.apptolast.menus.consumer.model.entity.UserFavoriteRestaurant
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.consumer.repository.UserFavoriteRestaurantRepository
import com.apptolast.menus.consumer.service.FavoriteRestaurantService
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FavoriteRestaurantServiceImpl(
    private val userFavoriteRestaurantRepository: UserFavoriteRestaurantRepository,
    private val restaurantRepository: RestaurantRepository,
    private val userAccountRepository: UserAccountRepository
) : FavoriteRestaurantService {

    private val log = LoggerFactory.getLogger(FavoriteRestaurantServiceImpl::class.java)

    override fun getFavorites(userId: UUID): List<FavoriteRestaurantResponse> {
        log.info("Getting favorite restaurants for user {}", userId)
        return userFavoriteRestaurantRepository.findByUserIdWithRestaurant(userId)
            .map { it.toResponse() }
    }

    @Transactional
    override fun addFavorite(userId: UUID, restaurantId: UUID) {
        log.info("Adding restaurant {} to favorites for user {}", restaurantId, userId)

        if (userFavoriteRestaurantRepository.existsByUserIdAndRestaurantId(userId, restaurantId)) {
            throw ConflictException("ALREADY_FAVORITE", "Restaurant is already in favorites")
        }

        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("RESTAURANT_NOT_FOUND", "Restaurant not found") }

        userFavoriteRestaurantRepository.save(
            UserFavoriteRestaurant(user = user, restaurant = restaurant)
        )
    }

    @Transactional
    override fun removeFavorite(userId: UUID, restaurantId: UUID) {
        log.info("Removing restaurant {} from favorites for user {}", restaurantId, userId)
        userFavoriteRestaurantRepository.deleteByUserIdAndRestaurantId(userId, restaurantId)
    }

    override fun isFavorite(userId: UUID, restaurantId: UUID): Boolean =
        userFavoriteRestaurantRepository.existsByUserIdAndRestaurantId(userId, restaurantId)

    private fun UserFavoriteRestaurant.toResponse() = FavoriteRestaurantResponse(
        restaurantId = restaurant.id,
        restaurantName = restaurant.name,
        restaurantSlug = restaurant.slug
    )
}
