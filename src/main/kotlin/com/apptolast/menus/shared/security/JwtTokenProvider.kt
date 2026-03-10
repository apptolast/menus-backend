package com.apptolast.menus.shared.security

import com.apptolast.menus.config.AppConfig
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
        Keys.hmacShaKeyFor(appConfig.jwt.secret.toByteArray(Charsets.UTF_8))
    }

    fun generateAccessToken(userId: UUID, profileUuid: UUID, role: String, tenantId: UUID? = null): String {
        val now = Date()
        val expiry = Date(now.time + appConfig.jwt.accessExpiration * 1000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("type", "access")
            .claim("profileUuid", profileUuid.toString())
            .apply { tenantId?.let { claim("tenantId", it.toString()) } }
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

    fun validateToken(token: String): Boolean {
        return runCatching { getClaims(token) }.isSuccess
    }

    fun getUserIdFromToken(token: String): UUID =
        UUID.fromString(getClaims(token).subject)

    fun getRoleFromToken(token: String): String =
        getClaims(token)["role"] as? String ?: ""

    fun getProfileUuidFromToken(token: String): UUID? =
        (getClaims(token)["profileUuid"] as? String)?.let { UUID.fromString(it) }

    fun getTenantIdFromToken(token: String): UUID? =
        (getClaims(token)["tenantId"] as? String)?.let { UUID.fromString(it) }

    fun getTokenType(token: String): String =
        getClaims(token)["type"] as? String ?: ""

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
