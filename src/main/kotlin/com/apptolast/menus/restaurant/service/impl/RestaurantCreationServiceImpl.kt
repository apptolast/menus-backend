package com.apptolast.menus.restaurant.service.impl

import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.model.entity.Subscription
import com.apptolast.menus.restaurant.model.enum.SubscriptionTier
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.restaurant.service.RestaurantCreationResult
import com.apptolast.menus.restaurant.service.RestaurantCreationService
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class RestaurantCreationServiceImpl(
    private val restaurantRepository: RestaurantRepository,
    private val userAccountRepository: UserAccountRepository,
    private val entityManager: EntityManager
) : RestaurantCreationService {

    private val logger = LoggerFactory.getLogger(RestaurantCreationServiceImpl::class.java)

    override fun createRestaurantForCurrentUser(
        userId: UUID,
        profileUuid: UUID,
        currentRole: UserRole,
        name: String,
        slug: String?
    ): RestaurantCreationResult {
        if (currentRole == UserRole.ADMIN) {
            throw ForbiddenException("ADMIN_CANNOT_OWN_RESTAURANT", "Admin users cannot own a restaurant")
        }

        if (restaurantRepository.existsByOwnerId(userId)) {
            throw ConflictException("RESTAURANT_ALREADY_EXISTS", "User already owns a restaurant")
        }

        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }

        val finalSlug = slug ?: generateSlug(name)
        if (restaurantRepository.existsBySlug(finalSlug)) {
            throw ConflictException("SLUG_ALREADY_EXISTS", "Restaurant slug '$finalSlug' is already taken")
        }

        val tenantId = UUID.randomUUID()
        val restaurant = Restaurant(
            tenantId = tenantId,
            ownerId = userId,
            name = name,
            slug = finalSlug
        )
        entityManager.persist(restaurant)

        val subscription = Subscription(
            restaurantId = restaurant.id,
            tier = SubscriptionTier.BASIC,
            maxMenus = 1,
            maxDishes = 50
        )
        entityManager.persist(subscription)

        if (user.role != UserRole.RESTAURANT_OWNER) {
            user.role = UserRole.RESTAURANT_OWNER
            logger.info("Promoted user id={} from {} to RESTAURANT_OWNER", userId, currentRole)
        }

        logger.info("Created restaurant id={} with tenant={} for user id={}", restaurant.id, tenantId, userId)

        return RestaurantCreationResult(
            restaurant = restaurant,
            user = user,
            tenantId = tenantId
        )
    }

    private fun generateSlug(name: String): String =
        name.lowercase()
            .replace(Regex("[\u00e1\u00e0\u00e4\u00e2]"), "a")
            .replace(Regex("[\u00e9\u00e8\u00eb\u00ea]"), "e")
            .replace(Regex("[\u00ed\u00ec\u00ef\u00ee]"), "i")
            .replace(Regex("[\u00f3\u00f2\u00f6\u00f4]"), "o")
            .replace(Regex("[\u00fa\u00f9\u00fc\u00fb]"), "u")
            .replace(Regex("[\u00f1]"), "n")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(100)
}
