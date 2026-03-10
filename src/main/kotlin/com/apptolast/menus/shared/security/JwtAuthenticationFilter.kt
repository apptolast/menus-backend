package com.apptolast.menus.shared.security

import com.apptolast.menus.consumer.model.enum.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null && jwtTokenProvider.validateToken(token) &&
            jwtTokenProvider.getTokenType(token) == "access"
        ) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val profileUuid = jwtTokenProvider.getProfileUuidFromToken(token)
            val roleStr = jwtTokenProvider.getRoleFromToken(token)
            val tenantId = jwtTokenProvider.getTenantIdFromToken(token)

            val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull()
            if (profileUuid == null || role == null) {
                log.warn("Valid JWT signature but missing or invalid claims: profileUuid={}, role='{}'", profileUuid, roleStr)
            } else {
                val principal = UserPrincipal(
                    userId = userId,
                    profileUuid = profileUuid,
                    role = role,
                    tenantId = tenantId,
                    _username = userId.toString()
                )
                val authentication = UsernamePasswordAuthenticationToken(
                    principal, null, principal.authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}
