package com.pawmotionai.configuration

import com.pawmotionai.identity.application.port.out.OwnerRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
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
            if (account == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown account")
                return
            }
            if (!account.isActive) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is disabled")
                return
            }
            // The database role is the authority; JWT "roles" claims are never trusted (C15).
            val refreshed = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_${account.role.name}")))
            refreshed.details = authentication.details
            SecurityContextHolder.getContext().authentication = refreshed
        }
        filterChain.doFilter(request, response)
    }
}
