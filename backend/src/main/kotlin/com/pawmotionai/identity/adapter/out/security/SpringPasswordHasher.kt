package com.pawmotionai.identity.adapter.out.security

import com.pawmotionai.identity.application.port.out.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SpringPasswordHasher(private val passwordEncoder: PasswordEncoder) : PasswordHasher {

    override fun hash(rawPassword: String): String = requireNotNull(passwordEncoder.encode(rawPassword))

    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        passwordEncoder.matches(rawPassword, encodedPassword)
}
