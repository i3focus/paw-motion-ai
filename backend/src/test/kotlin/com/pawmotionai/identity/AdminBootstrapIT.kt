package com.pawmotionai.identity

import com.pawmotionai.identity.application.usecase.AdminBootstrapRunner
import com.pawmotionai.support.ApiIntegrationTest
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.jdbc.core.simple.JdbcClient
import java.util.UUID

/** C15: promotion to ADMIN only by explicit bootstrap; MANUAL admins are kept. Decision 3.3. */
class AdminBootstrapIT : ApiIntegrationTest() {

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    private fun runner(emails: String, enabled: Boolean, reconcile: Boolean = false) =
        AdminBootstrapRunner(jdbcClient, SimpleMeterRegistry(), emails, enabled, reconcile)

    private fun emailOf(id: UUID) = jdbc.queryForObject("SELECT email FROM owners WHERE id = ?", String::class.java, id)!!
    private fun roleOf(id: UUID) = jdbc.queryForObject("SELECT role FROM owners WHERE id = ?", String::class.java, id)!!
    private fun sourceOf(id: UUID) = jdbc.queryForObject("SELECT role_source FROM owners WHERE id = ?", String::class.java, id)!!

    @Test
    fun `migration keeps existing admins as MANUAL`() {
        val admin = registerOwner("Legacy", role = "ADMIN")
        assertEquals("MANUAL", sourceOf(admin.ownerId))
    }

    @Test
    fun `bootstrap disabled promotes nobody`() {
        val owner = registerOwner("Plain")
        runner(emailOf(owner.ownerId), enabled = false).run(DefaultApplicationArguments())
        assertEquals("OWNER", roleOf(owner.ownerId))
    }

    @Test
    fun `bootstrap enabled promotes listed email as CONFIG and reconcile demotes only CONFIG admins`() {
        val listed = registerOwner("Listed")
        val dropped = registerOwner("Dropped")
        val manual = registerOwner("Manual", role = "ADMIN")

        runner("${emailOf(listed.ownerId).uppercase()},${emailOf(dropped.ownerId)}", enabled = true)
            .run(DefaultApplicationArguments())
        assertEquals("ADMIN", roleOf(listed.ownerId))
        assertEquals("CONFIG", sourceOf(listed.ownerId))
        assertEquals("MANUAL", sourceOf(manual.ownerId))

        runner(emailOf(listed.ownerId), enabled = true, reconcile = true).run(DefaultApplicationArguments())
        assertEquals("ADMIN", roleOf(listed.ownerId))
        assertEquals("OWNER", roleOf(dropped.ownerId))
        assertEquals("ADMIN", roleOf(manual.ownerId))
    }

    @Test
    fun `listed email in config does not promote on login`() {
        val owner = registerOwner("LoginOnly")
        val before = roleOf(owner.ownerId)
        assertEquals("OWNER", before)
        // Login path no longer touches role; the admin endpoint stays closed for a non-admin account.
        assertEquals(403, call("GET", "/api/admin/campaigns", owner.token).status)
    }
}
