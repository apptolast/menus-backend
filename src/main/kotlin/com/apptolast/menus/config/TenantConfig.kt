package com.apptolast.menus.config

import com.apptolast.menus.shared.security.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.sql.DataSource

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
class TenantFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            val principal = authentication?.principal as? UserPrincipal
            if (principal?.tenantId != null) {
                TenantContext.setTenant(principal.tenantId.toString())
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}

@Configuration
class TenantDataSourceConfig {
    @Bean
    fun tenantDataSourceBeanPostProcessor(): BeanPostProcessor = TenantAwareDataSourcePostProcessor()
}

/**
 * Wraps the auto-configured DataSource so that [TenantContext] is applied to the DB session
 * via `set_config('app.current_tenant', ...)` on every connection checkout.
 * This propagates the RLS tenant context set in [TenantFilter] to PostgreSQL.
 */
internal class TenantAwareDataSourcePostProcessor : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (beanName == "dataSource" && bean is DataSource) {
            return TenantAwareDataSource(bean)
        }
        return bean
    }
}

internal class TenantAwareDataSource(private val delegate: DataSource) : DataSource by delegate {
    override fun getConnection() = applyTenant(delegate.getConnection())
    override fun getConnection(username: String, password: String) =
        applyTenant(delegate.getConnection(username, password))

    private fun applyTenant(conn: java.sql.Connection): java.sql.Connection {
        val tenant = TenantContext.getTenant()
        if (tenant != null) {
            // Validate UUID format to prevent injection
            val safeUuid = java.util.UUID.fromString(tenant).toString()
            conn.prepareStatement("SELECT set_config('app.current_tenant', ?, false)").use { stmt ->
                stmt.setString(1, safeUuid)
                stmt.execute()
            }
        } else {
            conn.prepareStatement("SELECT set_config('app.current_tenant', '', false)").use { stmt ->
                stmt.execute()
            }
        }
        return conn
    }
}
