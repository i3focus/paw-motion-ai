package com.pawmotionai.identity.application.usecase

import com.pawmotionai.shared.LogSafe
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

/**
 * Promove a ADMIN, no startup, os emails de `app.admin.emails`, somente com `app.admin.bootstrap-enabled=true` (C15).
 * A promocao grava `role_source='CONFIG'`. Com `app.admin.reconcile=true`, rebaixa apenas admins `CONFIG` fora da lista;
 * admins `MANUAL` nunca sao alterados. Logs usam so um hash curto do email.
 */
@Component
class AdminBootstrapRunner(
    private val jdbc: JdbcClient,
    private val meterRegistry: MeterRegistry,
    @param:Value("\${app.admin.emails:}") emails: String,
    @param:Value("\${app.admin.bootstrap-enabled:false}") private val bootstrapEnabled: Boolean,
    @param:Value("\${app.admin.reconcile:false}") private val reconcile: Boolean,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(AdminBootstrapRunner::class.java)

    private val adminEmails: Set<String> = emails.split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    override fun run(args: ApplicationArguments) {
        if (!bootstrapEnabled) return
        adminEmails.forEach { email ->
            val promoted = jdbc.sql(
                """
                UPDATE owners SET role = 'ADMIN', role_source = 'CONFIG', updated_at = now()
                WHERE lower(email) = :email AND role = 'OWNER' AND status = 'ACTIVE'
                """.trimIndent(),
            ).param("email", email).update()
            if (promoted > 0) record("promoted", email)
        }
        if (reconcile) {
            val configured = adminEmails.toList()
            val demoted = jdbc.sql(
                """
                SELECT id, email FROM owners
                WHERE role = 'ADMIN' AND role_source = 'CONFIG' AND NOT (lower(email) = ANY(:emails))
                """.trimIndent(),
            ).param("emails", configured.toTypedArray()).query { rs, _ -> rs.getObject("id") to rs.getString("email") }.list()
            demoted.forEach { (id, email) ->
                jdbc.sql("UPDATE owners SET role = 'OWNER', role_source = 'MANUAL', updated_at = now() WHERE id = :id")
                    .param("id", id).update()
                record("demoted", email)
            }
        }
    }

    private fun record(action: String, email: String) {
        meterRegistry.counter("admin.role_changes", "action", action).increment()
        log.info("admin bootstrap {} emailHash={}", action, LogSafe.hash(email))
    }

}
