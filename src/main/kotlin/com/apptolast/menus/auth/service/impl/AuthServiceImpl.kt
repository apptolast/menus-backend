package com.apptolast.menus.auth.service.impl

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.service.AuthService
import com.apptolast.menus.auth.service.GoogleTokenVerifier
import com.apptolast.menus.config.EncryptionConfig
import com.apptolast.menus.consumer.model.entity.OAuthAccount
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.apptolast.menus.shared.security.JwtTokenProvider
import jakarta.persistence.EntityManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val encryptionConfig: EncryptionConfig,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val entityManager: EntityManager
) : AuthService {

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
        return buildAuthResponse(user)
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

    private fun buildAuthResponse(user: UserAccount): AuthResponse =
        AuthResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id, user.role.name, user.profileUuid),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id),
            expiresIn = 900
        )
}
