package com.apptolast.menus.config

import com.apptolast.menus.consumer.model.enum.UserRole
import com.apptolast.menus.restaurant.repository.RestaurantRepository
import com.apptolast.menus.shared.security.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
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
@Order(10)
class TenantFilter(private val restaurantRepository: RestaurantRepository) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val auth = SecurityContextHolder.getContext().authentication
            if (auth != null && auth.isAuthenticated && auth.principal is UserPrincipal) {
                val principal = auth.principal as UserPrincipal
                if (principal.role == UserRole.RESTAURANT_OWNER) {
                    restaurantRepository.findByOwnerId(principal.userId).ifPresent { restaurant ->
                        TenantContext.setTenant(restaurant.tenantId.toString())
                    }
                }
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
