package com.familyreunion.rsvp.security

import com.familyreunion.rsvp.repository.AdminUserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class GoogleAuthFilter(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val adminUserRepository: AdminUserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val userInfo = googleTokenVerifier.verify(token)
            if (userInfo != null && adminUserRepository.existsByEmail(userInfo.email)) {
                val auth = UsernamePasswordAuthenticationToken(
                    userInfo,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                )
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }
}
