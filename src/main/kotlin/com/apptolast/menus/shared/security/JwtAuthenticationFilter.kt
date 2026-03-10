package com.apptolast.menus.shared.security

import com.apptolast.menus.consumer.repository.UserAccountRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userAccountRepository: UserAccountRepository
) : OncePerRequestFilter() {

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
            userAccountRepository.findById(userId).ifPresent { user ->
                if (user.isActive) {
                    val principal = UserPrincipal(
                        userId = user.id,
                        profileUuid = user.profileUuid,
                        role = user.role,
                        _username = userId.toString()
                    )
                    val authentication = UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
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
