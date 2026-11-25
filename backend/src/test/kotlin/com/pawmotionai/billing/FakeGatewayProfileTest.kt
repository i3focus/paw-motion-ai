package com.pawmotionai.billing

import com.pawmotionai.billing.adapter.out.payment.FakePaymentGatewayAdapter
import com.pawmotionai.billing.application.port.out.PaymentGatewayPort
import com.pawmotionai.configuration.BillingProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals

/** N5: the fake gateway (and so the fake webhook route) exists only under the `local` profile. */
class FakeGatewayProfileTest {
    private val runner = ApplicationContextRunner()
        .withBean(BillingProperties::class.java)
        .withUserConfiguration(FakePaymentGatewayAdapter::class.java)
        .withPropertyValues("app.billing.provider=fake")

    @Test
    fun `fake gateway is not registered outside the local profile`() {
        runner.withInitializer { it.environment.setActiveProfiles("prod") }
            .run { assertEquals(0, it.getBeansOfType(PaymentGatewayPort::class.java).size) }
        runner.run { assertEquals(0, it.getBeansOfType(PaymentGatewayPort::class.java).size) }
    }

    @Test
    fun `fake gateway is registered under the local profile`() {
        runner.withInitializer { it.environment.setActiveProfiles("local") }
            .run { assertEquals(1, it.getBeansOfType(PaymentGatewayPort::class.java).size) }
    }
}
