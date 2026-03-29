package com.apptolast.menus.auth.service.impl

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.dto.response.AuthResponse
import com.apptolast.menus.auth.service.AdminWhitelistService
import com.apptolast.menus.auth.service.AuthService
import com.apptolast.menus.auth.service.GoogleTokenVerifier
import com.apptolast.menus.config.AppConfig
import com.apptolast.menus.consumer.model.entity.OAuthAccount
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import com.apptolast.menus.shared.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val oAuthAccountRepository: OAuthAccountRepository,
    private val adminWhitelistService: AdminWhitelistService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val appConfig: AppConfig
) : AuthService {

    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    @Transactional
    override fun register(request: RegisterRequest): AuthResponse {
        if (userAccountRepository.existsByEmail(request.email)) {
            throw ConflictException("EMAIL_ALREADY_EXISTS", "An account with this email already exists")
        }

        val user = userAccountRepository.save(
            UserAccount(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                name = request.name,
                role = UserRole.USER
            )
        )

        logger.info("Registered new user id={}", user.id)
        return buildAuthResponse(user)
    }

    @Transactional
    override fun registerAdmin(request: RegisterAdminRequest): AuthResponse {
        if (!adminWhitelistService.isWhitelisted(request.email)) {
            throw ForbiddenException("EMAIL_NOT_WHITELISTED", "This email is not authorized for admin registration")
        }

        if (userAccountRepository.existsByEmail(request.email)) {
            throw ConflictException("EMAIL_ALREADY_EXISTS", "An account with this email already exists")
        }

        val user = userAccountRepository.save(
            UserAccount(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                name = request.name,
                role = UserRole.ADMIN
            )
        )

        logger.info("Registered new admin user id={}", user.id)
        return buildAuthResponse(user)
    }

    @Transactional(readOnly = true)
    override fun login(request: LoginRequest): AuthResponse {
        val user = userAccountRepository.findByEmail(request.email)
            .orElseThrow { BadCredentialsException("Invalid credentials") }

        val hash = user.passwordHash
            ?: throw BadCredentialsException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, hash)) {
            throw BadCredentialsException("Invalid credentials")
        }

        return buildAuthResponse(user)
    }

    @Transactional(readOnly = true)
    override fun refresh(refreshToken: String): AuthResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ForbiddenException("INVALID_REFRESH_TOKEN", "Invalid or expired refresh token")
        }

        if (jwtTokenProvider.getTokenType(refreshToken) != "refresh") {
            throw ForbiddenException("INVALID_TOKEN_TYPE", "Token is not a refresh token")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val user = userAccountRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }

        return buildAuthResponse(user)
    }

    @Transactional
    override fun loginWithGoogle(idToken: String): AuthResponse {
        val googleUser = runCatching { googleTokenVerifier.verify(idToken) }
            .getOrElse {
                throw ForbiddenException("INVALID_GOOGLE_TOKEN", "Invalid Google ID token")
            }

        val existingOAuth = oAuthAccountRepository.findByProviderAndProviderId("GOOGLE", googleUser.googleId)

        val user = if (existingOAuth.isPresent) {
            userAccountRepository.findById(existingOAuth.get().userId)
                .orElseThrow { ResourceNotFoundException("USER_NOT_FOUND", "User not found") }
        } else {
            val existingUser = userAccountRepository.findByEmail(googleUser.email)
            val userAccount = if (existingUser.isPresent) {
                existingUser.get()
            } else {
                userAccountRepository.save(
                    UserAccount(
                        email = googleUser.email,
                        name = googleUser.name,
                        role = UserRole.USER
                    )
                )
            }

            oAuthAccountRepository.save(
                OAuthAccount(
                    userId = userAccount.id,
                    provider = "GOOGLE",
                    providerId = googleUser.googleId,
                    email = googleUser.email
                )
            )

            userAccount
        }

        logger.info("Google login for user id={}", user.id)
        return buildAuthResponse(user)
    }

    private fun buildAuthResponse(user: UserAccount): AuthResponse =
        AuthResponse(
            accessToken = jwtTokenProvider.generateAccessToken(user.id, user.role),
            refreshToken = jwtTokenProvider.generateRefreshToken(user.id),
            tokenType = "Bearer",
            expiresIn = appConfig.jwt.accessExpiration
        )
}
