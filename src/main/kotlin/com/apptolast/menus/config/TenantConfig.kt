package com.apptolast.menus.config

import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.security.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.filter.OncePerRequestFilter

object TenantContext {
    private val currentTenant = ThreadLocal<String?>()

    fun setTenant(tenantId: String?) {
        currentTenant.set(tenantId)
    }

    fun getTenant(): String? = currentTenant.get()

    fun clear() {
        currentTenant.remove()
    }
}

@Order(1)
@Component
class TenantFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val restaurantRepository: RestaurantRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(TenantFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractBearerToken(request)
            if (token != null && jwtTokenProvider.validateToken(token) &&
                jwtTokenProvider.getTokenType(token) == "access"
            ) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                restaurantRepository.findByOwnerId(userId).ifPresent { restaurant ->
                    TenantContext.setTenant(restaurant.tenantId.toString())
                }
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }
}

@Aspect
@Configuration
class TenantAspect(private val jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate) {

    private val log = LoggerFactory.getLogger(TenantAspect::class.java)

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    fun applyTenantToSession() {
        val tenantId = TenantContext.getTenant() ?: return
        try {
            jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, true)",
                String::class.java,
                tenantId
            )
        } catch (ex: Exception) {
            log.debug("Could not set tenant session variable: {}", ex.message)
        }
    }
}
