package com.pawmotionai.configuration

import com.pawmotionai.identity.application.port.out.OwnerRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class ActiveAccountFilter(private val ownerRepository: OwnerRepository) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authentication = SecurityContextHolder.getContext().authentication
        val jwt = authentication?.principal as? Jwt
        if (jwt != null) {
            val account = try {
                ownerRepository.findById(UUID.fromString(jwt.subject))
            } catch (exception: IllegalArgumentException) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid account subject")
                return
            }
            if (account == null || !account.isActive) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is disabled")
                return
            }
        }
        filterChain.doFilter(request, response)
    }
}
