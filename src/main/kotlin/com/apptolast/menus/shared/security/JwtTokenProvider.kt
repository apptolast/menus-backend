package com.apptolast.menus.shared.security

import com.apptolast.menus.config.AppConfig
import com.apptolast.menus.consumer.model.enum.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(private val appConfig: AppConfig) {

    private val signingKey: SecretKey by lazy {
        val bytes = appConfig.jwt.secret.toByteArray(Charsets.UTF_8)
        require(bytes.size >= 64) {
            "JWT secret must be at least 64 bytes for HS512, but was ${bytes.size} bytes."
        }
        Keys.hmacShaKeyFor(bytes)
    }

    fun generateAccessToken(userId: UUID, role: UserRole): String {
        val now = Date()
        val expiry = Date(now.time + appConfig.jwt.accessExpiration * 1000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiry = Date(now.time + appConfig.jwt.refreshExpiration * 1000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        runCatching { getClaims(token) }.isSuccess

    fun getUserIdFromToken(token: String): UUID =
        UUID.fromString(getClaims(token).subject)

    fun getRoleFromToken(token: String): String =
        getClaims(token)["role"] as? String ?: ""

    fun getTokenType(token: String): String =
        getClaims(token)["type"] as? String ?: ""

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
