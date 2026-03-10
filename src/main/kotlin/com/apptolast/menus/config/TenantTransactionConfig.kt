package com.apptolast.menus.config

import jakarta.persistence.EntityManagerFactory
import org.hibernate.Session
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.EntityManagerFactoryUtils
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.TransactionDefinition
import java.util.UUID

@Configuration
class TenantTransactionConfig {

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager =
        TenantAwareJpaTransactionManager(entityManagerFactory)
}

class TenantAwareJpaTransactionManager(emf: EntityManagerFactory) : JpaTransactionManager(emf) {

    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        super.doBegin(transaction, definition)
        val tenantId = TenantContext.getTenant()?.takeIf { isValidUuid(it) } ?: return
        val em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory!!) ?: return
        em.unwrap(Session::class.java).doWork { connection ->
            connection.prepareStatement("SELECT set_config('app.current_tenant', ?, true)").use { stmt ->
                stmt.setString(1, tenantId)
                stmt.execute()
            }
        }
    }

    private fun isValidUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess
}
