package com.apptolast.menus.auth.service

import com.apptolast.menus.auth.dto.request.LoginRequest
import com.apptolast.menus.auth.dto.request.RegisterRequest
import com.apptolast.menus.auth.service.impl.AuthServiceImpl
import com.apptolast.menus.config.EncryptionConfig
import com.apptolast.menus.consumer.model.entity.UserAccount
import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.consumer.repository.OAuthAccountRepository
import com.apptolast.menus.consumer.repository.UserAccountRepository
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
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
    @Mock lateinit var jwtTokenProvider: JwtTokenProvider
    @Mock lateinit var encryptionConfig: EncryptionConfig
    @Mock lateinit var googleTokenVerifier: GoogleTokenVerifier

    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthServiceImpl(
            userAccountRepository,
            oAuthAccountRepository,
            jwtTokenProvider,
            passwordEncoder,
            encryptionConfig,
            googleTokenVerifier
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyMatcher(clazz: Class<T>): T =
        org.mockito.ArgumentMatchers.any(clazz) ?: (null as T)

    @Test
    @DisplayName("register: creates user and returns tokens when email is new")
    fun registerSuccess() {
        val email = "test@example.com"
        val hash = "abc123hash"
        val encrypted = "encrypted".toByteArray()
        val savedUser = UserAccount(email = encrypted, emailHash = hash, role = UserRole.CONSUMER)

        `when`(encryptionConfig.hashEmail(email)).thenReturn(hash)
        `when`(encryptionConfig.encryptEmail(email)).thenReturn(encrypted)
        `when`(userAccountRepository.existsByEmailHash(hash)).thenReturn(false)
        `when`(userAccountRepository.save(anyMatcher(UserAccount::class.java))).thenReturn(savedUser)
        `when`(jwtTokenProvider.generateAccessToken(anyMatcher(UUID::class.java), anyString())).thenReturn("access-token")
        `when`(jwtTokenProvider.generateRefreshToken(anyMatcher(UUID::class.java))).thenReturn("refresh-token")

        val response = authService.register(RegisterRequest(email, "Password1!"))

        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
        assertThat(response.tokenType).isEqualTo("Bearer")
    }

    @Test
    @DisplayName("register: throws ConflictException when email already exists")
    fun registerDuplicateEmail() {
        val email = "existing@example.com"
        val hash = "existinghash"
        `when`(encryptionConfig.hashEmail(email)).thenReturn(hash)
        `when`(userAccountRepository.existsByEmailHash(hash)).thenReturn(true)

        assertThatThrownBy {
            authService.register(RegisterRequest(email, "Password1!"))
        }.isInstanceOf(ConflictException::class.java)
            .satisfies({ ex ->
                assertThat((ex as ConflictException).errorCode).isEqualTo("EMAIL_ALREADY_EXISTS")
            })
    }

    @Test
    @DisplayName("login: returns tokens for valid credentials")
    fun loginSuccess() {
        val email = "user@example.com"
        val password = "Password1!"
        val hash = passwordEncoder.encode(password)
        val emailHash = "hashvalue"
        val encrypted = "enc".toByteArray()
        val user = UserAccount(email = encrypted, emailHash = emailHash, passwordHash = hash, role = UserRole.CONSUMER)

        `when`(encryptionConfig.hashEmail(email)).thenReturn(emailHash)
        `when`(userAccountRepository.findByEmailHash(emailHash)).thenReturn(Optional.of(user))
        `when`(jwtTokenProvider.generateAccessToken(anyMatcher(UUID::class.java), anyString())).thenReturn("access-token")
        `when`(jwtTokenProvider.generateRefreshToken(anyMatcher(UUID::class.java))).thenReturn("refresh-token")

        val response = authService.login(LoginRequest(email, password))
        assertThat(response.accessToken).isEqualTo("access-token")
    }

    @Test
    @DisplayName("login: throws exception for wrong password")
    fun loginWrongPassword() {
        val email = "user@example.com"
        val emailHash = "hashvalue"
        val correctHash = passwordEncoder.encode("correct-password")
        val user = UserAccount(
            email = "enc".toByteArray(),
            emailHash = emailHash,
            passwordHash = correctHash,
            role = UserRole.CONSUMER
        )

        `when`(encryptionConfig.hashEmail(email)).thenReturn(emailHash)
        `when`(userAccountRepository.findByEmailHash(emailHash)).thenReturn(Optional.of(user))

        assertThatThrownBy {
            authService.login(LoginRequest(email, "wrong-password"))
        }.isInstanceOf(BadCredentialsException::class.java)
    }
}
