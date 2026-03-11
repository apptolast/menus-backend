package com.apptolast.menus.admin.service.impl

import com.apptolast.menus.admin.service.AdminUserService
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminUserServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val restaurantRepository: RestaurantRepository
) : AdminUserService {

    private val logger = LoggerFactory.getLogger(AdminUserServiceImpl::class.java)

    override fun listUsers(role: UserRole?, pageable: Pageable): Page<UserAccount> {
        return if (role != null) {
            userAccountRepository.findByRole(role, pageable)
        } else {
            userAccountRepository.findAll(pageable)
        }
    }

    @Transactional
    override fun changeRole(userId: UUID, newRole: UserRole): UserAccount {
        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User with id $userId not found") }

        if (user.role == newRole) {
            return user
        }

        if (newRole == UserRole.RESTAURANT_OWNER && !restaurantRepository.existsByOwnerId(userId)) {
            throw ConflictException(
                "ROLE_CHANGE_REQUIRES_RESTAURANT",
                "Cannot assign RESTAURANT_OWNER role without an existing restaurant"
            )
        }

        if (user.role == UserRole.RESTAURANT_OWNER && newRole != UserRole.RESTAURANT_OWNER) {
            if (restaurantRepository.existsByOwnerId(userId)) {
                throw ForbiddenException(
                    "CANNOT_DEMOTE_RESTAURANT_OWNER",
                    "Cannot change role from RESTAURANT_OWNER while user owns an active restaurant"
                )
            }
        }

        val oldRole = user.role
        user.role = newRole
        user.updatedAt = OffsetDateTime.now()

        logger.info("Changed role for user id={} from {} to {}", userId, oldRole, newRole)

        return userAccountRepository.save(user)
    }
}
