package com.apptolast.menus.config

import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.security.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
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

@Component
class TenantFilter(
    private val restaurantRepository: RestaurantRepository,
    private val jdbcTemplate: JdbcTemplate
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val principal = SecurityContextHolder.getContext().authentication
                ?.principal as? UserPrincipal
            if (principal != null) {
                restaurantRepository.findByOwnerId(principal.userId)
                    .ifPresent { restaurant ->
                        val tenantId = restaurant.tenantId.toString()
                        TenantContext.setTenant(tenantId)
                        jdbcTemplate.execute("SET LOCAL app.current_tenant = '$tenantId'")
                    }
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
