package com.apptolast.menus.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.PreparedStatement

/**
 * AOP aspect that propagates the current tenant to PostgreSQL's session variable
 * `app.current_tenant` within each @Transactional method.
 *
 * The aspect runs INSIDE the @Transactional boundary (lowest precedence = innermost wrapper)
 * so `set_config(..., true)` (transaction-local) takes effect for the active transaction.
 */
@Aspect
@Component
@Order(Int.MAX_VALUE)
class TenantTransactionAspect(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    fun propagateTenant(joinPoint: ProceedingJoinPoint): Any? {
        val tenant = TenantContext.getTenant()
        if (tenant != null) {
            try {
                jdbcTemplate.execute(
                    "SELECT set_config('app.current_tenant', ?, true)"
                ) { ps: PreparedStatement ->
                    ps.setString(1, tenant)
                    ps.execute()
                }
            } catch (ex: Exception) {
                logger.warn("Failed to set tenant context in DB: ${ex.message}")
            }
        }
        return joinPoint.proceed()
    }
}
