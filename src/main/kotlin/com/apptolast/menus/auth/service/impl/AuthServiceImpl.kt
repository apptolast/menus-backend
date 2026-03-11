package com.apptolast.menus.auth.service.impl

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.request.RegisterRestaurantRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.service.AuthService
import com.apptolast.menus.auth.service.GoogleTokenVerifier
import com.apptolast.menus.config.EncryptionConfig
import com.apptolast.menus.consumer.model.entity.OAuthAccount
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.restaurant.model.entity.Restaurant
import com.apptolast.menus.restaurant.model.entity.Subscription
import com.apptolast.menus.restaurant.model.enum.SubscriptionTier
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.exception.BusinessValidationException
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.apptolast.menus.shared.security.JwtTokenProvider
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class AuthServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val restaurantRepository: RestaurantRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val encryptionConfig: EncryptionConfig,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val entityManager: EntityManager
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    override fun register(request: RegisterRequest): AuthResponse {
        val emailHash = encryptionConfig.hashEmail(request.email)
        if (userAccountRepository.existsByEmailHash(emailHash)) {
            throw ConflictException("EMAIL_ALREADY_EXISTS", "Email is already registered")
        }
        val user = UserAccount(
            email = encryptionConfig.encryptEmail(request.email),
            emailHash = emailHash,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.CONSUMER
        )
        entityManager.persist(user)
        logger.info("Registered new consumer user id={}", user.id)
        return buildAuthResponse(user)
    }

    override fun registerRestaurant(request: RegisterRestaurantRequest): AuthResponse {
        if (!request.acceptTerms) {
            throw BusinessValidationException("TERMS_NOT_ACCEPTED", "You must accept the terms of service")
        }

        val emailHash = encryptionConfig.hashEmail(request.email)
        val existingUser = userAccountRepository.findByEmailHash(emailHash)

        if (existingUser.isPresent) {
            val user = existingUser.get()
            if (restaurantRepository.existsByOwnerId(user.id)) {
                throw ConflictException("EMAIL_ALREADY_EXISTS", "This email already has a restaurant")
            }
            val hash = user.passwordHash
                ?: throw BadCredentialsException("Invalid credentials")
            if (!passwordEncoder.matches(request.password, hash)) {
                throw BadCredentialsException("Invalid credentials")
            }
            user.role = UserRole.RESTAURANT_OWNER
            val restaurant = createRestaurantForUser(user.id, request.restaurantName, request.slug)
            logger.info("Promoted user id={} to RESTAURANT_OWNER with tenant={}", user.id, restaurant.tenantId)
            return buildAuthResponse(user, restaurant.tenantId)
        }

        val user = UserAccount(
            email = encryptionConfig.encryptEmail(request.email),
            emailHash = emailHash,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.RESTAURANT_OWNER
        )
        entityManager.persist(user)
        val restaurant = createRestaurantForUser(user.id, request.restaurantName, request.slug)
        logger.info("Registered new restaurant owner user id={} with tenant={}", user.id, restaurant.tenantId)
        return buildAuthResponse(user, restaurant.tenantId)
    }

    override fun login(request: LoginRequest): AuthResponse {
        val emailHash = encryptionConfig.hashEmail(request.email)
        val user = userAccountRepository.findByEmailHash(emailHash)
            .orElseThrow { BadCredentialsException("Invalid credentials") }
        if (!user.isActive) {
            throw ForbiddenException("ACCOUNT_DISABLED", "Account is disabled")
        }
        val hash = user.passwordHash
            ?: throw BadCredentialsException("Invalid credentials")
        if (!passwordEncoder.matches(request.password, hash)) {
            throw BadCredentialsException("Invalid credentials")
        }
        return buildAuthResponse(user)
    }

    override fun refresh(refreshToken: String): AuthResponse {
        if (!jwtTokenProvider.validateToken(refreshToken) ||
            jwtTokenProvider.getTokenType(refreshToken) != "refresh"
        ) {
            throw ForbiddenException("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token")
        }
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        if (!user.isActive) {
            throw ForbiddenException("ACCOUNT_DISABLED", "Account is disabled")
        }
        return buildAuthResponse(user)
    }

    override fun loginWithGoogle(idToken: String): AuthResponse {
        val googleUser = runCatching { googleTokenVerifier.verify(idToken) }
            .getOrElse {
                throw ForbiddenException("INVALID_GOOGLE_TOKEN", "Invalid Google ID token")
            }

        val existingOAuth = oAuthAccountRepository
            .findByProviderAndProviderId("GOOGLE", googleUser.googleId)

        val user = if (existingOAuth.isPresent) {
            userAccountRepository.findById(existingOAuth.get().userId)
                .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        } else {
            val emailHash = encryptionConfig.hashEmail(googleUser.email)
            val existingUser = userAccountRepository.findByEmailHash(emailHash)
            if (existingUser.isPresent) {
                val u = existingUser.get()
                val oauth = OAuthAccount(
                    userId = u.id,
                    provider = "GOOGLE",
                    providerId = googleUser.googleId,
                    email = encryptionConfig.encryptEmail(googleUser.email)
                )
                entityManager.persist(oauth)
                u
            } else {
                val newUser = UserAccount(
                    email = encryptionConfig.encryptEmail(googleUser.email),
                    emailHash = emailHash,
                    passwordHash = null,
                    role = UserRole.CONSUMER
                )
                entityManager.persist(newUser)
                val oauthAccount = OAuthAccount(
                    userId = newUser.id,
                    provider = "GOOGLE",
                    providerId = googleUser.googleId,
                    email = encryptionConfig.encryptEmail(googleUser.email)
                )
                entityManager.persist(oauthAccount)
                newUser
            }
        }
        if (!user.isActive) {
            throw ForbiddenException("ACCOUNT_DISABLED", "Account is disabled")
        }
        return buildAuthResponse(user)
    }

    private fun buildAuthResponse(user: UserAccount): AuthResponse {
        val tenantId = if (user.role == UserRole.RESTAURANT_OWNER) {
            restaurantRepository.findByOwnerId(user.id).map { it.tenantId }.orElse(null)
        } else {
            null
        }
        return buildAuthResponse(user, tenantId)
    }

    private fun buildAuthResponse(user: UserAccount, tenantId: UUID?): AuthResponse =
        AuthResponse(
            accessToken = jwtTokenProvider.generateAccessToken(
                user.id, user.role.name, user.profileUuid, tenantId
            ),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id),
            expiresIn = 900
        )

    private fun createRestaurantForUser(ownerId: UUID, name: String, slug: String?): Restaurant {
        val finalSlug = slug ?: generateSlug(name)
        if (restaurantRepository.existsBySlug(finalSlug)) {
            throw ConflictException("SLUG_ALREADY_EXISTS", "Restaurant slug '$finalSlug' is already taken")
        }
        val tenantId = UUID.randomUUID()
        val restaurant = Restaurant(
            tenantId = tenantId,
            ownerId = ownerId,
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
        return restaurant
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
