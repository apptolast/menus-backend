package com.apptolast.menus.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.JdbcTemplate
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
class TenantFilter(private val jdbcTemplate: JdbcTemplate) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
