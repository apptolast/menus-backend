package com.apptolast.menus.auth.service

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterAdminRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.service.impl.AuthServiceImpl
import com.apptolast.menus.config.AppConfig
import com.apptolast.menus.consumer.model.entity.OAuthAccount
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ForbiddenException
import com.apptolast.menus.shared.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    @Mock lateinit var userAccountRepository: UserAccountRepository
    @Mock lateinit var oAuthAccountRepository: OAuthAccountRepository
    @Mock lateinit var adminWhitelistService: AdminWhitelistService
    @Mock lateinit var jwtTokenProvider: JwtTokenProvider
    @Mock lateinit var googleTokenVerifier: GoogleTokenVerifier

    private val passwordEncoder = BCryptPasswordEncoder()
    private val appConfig = AppConfig(
        jwt = AppConfig.JwtProperties(
            secret = "test-secret-key-minimum-64-bytes-long-for-HS512-algorithm-testing!!",
            accessExpiration = 900L,
            refreshExpiration = 604800L
        )
    )
    private lateinit var authService: AuthService

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(clazz: Class<T>): T =
        org.mockito.ArgumentMatchers.any(clazz) ?: (null as T)

    @BeforeEach
    fun setUp() {
        authService = AuthServiceImpl(
            userAccountRepository,
            oAuthAccountRepository,
            adminWhitelistService,
            passwordEncoder,
            jwtTokenProvider,
            googleTokenVerifier,
            appConfig
        )
    }

    @Nested
    @DisplayName("register")
    inner class RegisterTests {

        @Test
        @DisplayName("creates USER account and returns tokens when email is new")
        fun registerSuccess() {
            val email = "test@example.com"
            val userId = UUID.randomUUID()
            val savedUser = UserAccount(id = userId, email = email, role = UserRole.USER)

            `when`(userAccountRepository.existsByEmail(email)).thenReturn(false)
            `when`(userAccountRepository.save(any(UserAccount::class.java))).thenReturn(savedUser)
            `when`(jwtTokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("access-token")
            `when`(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refresh-token")

            val response = authService.register(RegisterRequest(email, "Password1!"))

            assertThat(response.accessToken).isEqualTo("access-token")
            assertThat(response.refreshToken).isEqualTo("refresh-token")
            assertThat(response.tokenType).isEqualTo("Bearer")
        }

        @Test
        @DisplayName("throws ConflictException when email already exists")
        fun registerDuplicateEmail() {
            val email = "existing@example.com"
            `when`(userAccountRepository.existsByEmail(email)).thenReturn(true)

            assertThatThrownBy {
                authService.register(RegisterRequest(email, "Password1!"))
            }.isInstanceOf(ConflictException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ConflictException).errorCode).isEqualTo("EMAIL_ALREADY_EXISTS")
                })
        }
    }

    @Nested
    @DisplayName("registerAdmin")
    inner class RegisterAdminTests {

        @Test
        @DisplayName("creates ADMIN account when email is whitelisted and new")
        fun registerAdminSuccess() {
            val email = "admin@example.com"
            val userId = UUID.randomUUID()
            val savedUser = UserAccount(id = userId, email = email, role = UserRole.ADMIN)

            `when`(adminWhitelistService.isWhitelisted(email)).thenReturn(true)
            `when`(userAccountRepository.existsByEmail(email)).thenReturn(false)
            `when`(userAccountRepository.save(any(UserAccount::class.java))).thenReturn(savedUser)
            `when`(jwtTokenProvider.generateAccessToken(userId, UserRole.ADMIN)).thenReturn("admin-access-token")
            `when`(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("admin-refresh-token")

            val response = authService.registerAdmin(RegisterAdminRequest(email, "Password1!"))

            assertThat(response.accessToken).isEqualTo("admin-access-token")
        }

        @Test
        @DisplayName("throws ForbiddenException when email is not in whitelist")
        fun registerAdminEmailNotWhitelisted() {
            val email = "notwhitelisted@example.com"
            `when`(adminWhitelistService.isWhitelisted(email)).thenReturn(false)

            assertThatThrownBy {
                authService.registerAdmin(RegisterAdminRequest(email, "Password1!"))
            }.isInstanceOf(ForbiddenException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ForbiddenException).errorCode).isEqualTo("EMAIL_NOT_WHITELISTED")
                })
        }

        @Test
        @DisplayName("throws ConflictException when whitelisted email already registered")
        fun registerAdminDuplicateEmail() {
            val email = "admin-dup@example.com"
            `when`(adminWhitelistService.isWhitelisted(email)).thenReturn(true)
            `when`(userAccountRepository.existsByEmail(email)).thenReturn(true)

            assertThatThrownBy {
                authService.registerAdmin(RegisterAdminRequest(email, "Password1!"))
            }.isInstanceOf(ConflictException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ConflictException).errorCode).isEqualTo("EMAIL_ALREADY_EXISTS")
                })
        }
    }

    @Nested
    @DisplayName("login")
    inner class LoginTests {

        @Test
        @DisplayName("returns tokens for valid credentials")
        fun loginSuccess() {
            val email = "user@example.com"
            val password = "Password1!"
            val encodedPassword = passwordEncoder.encode(password)
            val userId = UUID.randomUUID()
            val user = UserAccount(id = userId, email = email, passwordHash = encodedPassword, role = UserRole.USER)

            `when`(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user))
            `when`(jwtTokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("access-token")
            `when`(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refresh-token")

            val response = authService.login(LoginRequest(email, password))

            assertThat(response.accessToken).isEqualTo("access-token")
            assertThat(response.refreshToken).isEqualTo("refresh-token")
        }

        @Test
        @DisplayName("throws BadCredentialsException for unknown email")
        fun loginUnknownEmail() {
            val email = "unknown@example.com"
            `when`(userAccountRepository.findByEmail(email)).thenReturn(Optional.empty())

            assertThatThrownBy {
                authService.login(LoginRequest(email, "Password1!"))
            }.isInstanceOf(BadCredentialsException::class.java)
        }

        @Test
        @DisplayName("throws BadCredentialsException for wrong password")
        fun loginWrongPassword() {
            val email = "user@example.com"
            val encodedPassword = passwordEncoder.encode("correct-password")
            val userId = UUID.randomUUID()
            val user = UserAccount(id = userId, email = email, passwordHash = encodedPassword, role = UserRole.USER)

            `when`(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user))

            assertThatThrownBy {
                authService.login(LoginRequest(email, "wrong-password"))
            }.isInstanceOf(BadCredentialsException::class.java)
        }

        @Test
        @DisplayName("throws BadCredentialsException when user has no password (OAuth-only account)")
        fun loginOAuthOnlyAccount() {
            val email = "oauth@example.com"
            val userId = UUID.randomUUID()
            val user = UserAccount(id = userId, email = email, passwordHash = null, role = UserRole.USER)

            `when`(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user))

            assertThatThrownBy {
                authService.login(LoginRequest(email, "anypassword"))
            }.isInstanceOf(BadCredentialsException::class.java)
        }
    }

    @Nested
    @DisplayName("loginWithGoogle")
    inner class GoogleLoginTests {

        @Test
        @DisplayName("creates new account and returns tokens for first-time Google login")
        fun googleLoginNewUser() {
            val googleUserInfo = GoogleUserInfo(
                googleId = "google-sub-123",
                email = "google.user@gmail.com",
                name = "Google User"
            )
            val userId = UUID.randomUUID()
            val savedUser = UserAccount(id = userId, email = googleUserInfo.email, role = UserRole.USER)

            `when`(googleTokenVerifier.verify("valid-id-token")).thenReturn(googleUserInfo)
            `when`(oAuthAccountRepository.findByProviderAndProviderId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty())
            `when`(userAccountRepository.findByEmail(googleUserInfo.email)).thenReturn(Optional.empty())
            `when`(userAccountRepository.save(any(UserAccount::class.java))).thenReturn(savedUser)
            `when`(oAuthAccountRepository.save(any(OAuthAccount::class.java)))
                .thenReturn(OAuthAccount(userId = userId, provider = "GOOGLE", providerId = "google-sub-123", email = googleUserInfo.email))
            `when`(jwtTokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("google-access-token")
            `when`(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("google-refresh-token")

            val response = authService.loginWithGoogle("valid-id-token")

            assertThat(response.accessToken).isEqualTo("google-access-token")
        }

        @Test
        @DisplayName("throws ForbiddenException for invalid Google ID token")
        fun googleLoginInvalidToken() {
            `when`(googleTokenVerifier.verify("invalid-token"))
                .thenThrow(IllegalArgumentException("Invalid token"))

            assertThatThrownBy {
                authService.loginWithGoogle("invalid-token")
            }.isInstanceOf(ForbiddenException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ForbiddenException).errorCode).isEqualTo("INVALID_GOOGLE_TOKEN")
                })
        }
    }

    @Nested
    @DisplayName("refresh")
    inner class RefreshTests {

        @Test
        @DisplayName("throws ForbiddenException for invalid refresh token")
        fun refreshInvalidToken() {
            `when`(jwtTokenProvider.validateToken("bad-token")).thenReturn(false)

            assertThatThrownBy {
                authService.refresh("bad-token")
            }.isInstanceOf(ForbiddenException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ForbiddenException).errorCode).isEqualTo("INVALID_REFRESH_TOKEN")
                })
        }

        @Test
        @DisplayName("throws ForbiddenException when token type is not refresh")
        fun refreshWrongTokenType() {
            `when`(jwtTokenProvider.validateToken("access-token")).thenReturn(true)
            `when`(jwtTokenProvider.getTokenType("access-token")).thenReturn("access")

            assertThatThrownBy {
                authService.refresh("access-token")
            }.isInstanceOf(ForbiddenException::class.java)
                .satisfies({ ex ->
                    assertThat((ex as ForbiddenException).errorCode).isEqualTo("INVALID_TOKEN_TYPE")
                })
        }

        @Test
        @DisplayName("returns new tokens for valid refresh token")
        fun refreshSuccess() {
            val userId = UUID.randomUUID()
            val user = UserAccount(id = userId, email = "user@example.com", role = UserRole.USER)

            `when`(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true)
            `when`(jwtTokenProvider.getTokenType("valid-refresh")).thenReturn("refresh")
            `when`(jwtTokenProvider.getUserIdFromToken("valid-refresh")).thenReturn(userId)
            `when`(userAccountRepository.findById(userId)).thenReturn(Optional.of(user))
            `when`(jwtTokenProvider.generateAccessToken(userId, UserRole.USER)).thenReturn("new-access-token")
            `when`(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("new-refresh-token")

            val response = authService.refresh("valid-refresh")

            assertThat(response.accessToken).isEqualTo("new-access-token")
        }
    }
}
