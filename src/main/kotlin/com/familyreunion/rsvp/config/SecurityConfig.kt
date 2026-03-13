package com.familyreunion.rsvp.config

import com.familyreunion.rsvp.security.GoogleAuthFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val googleAuthFilter: GoogleAuthFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                }
            }
            .authorizeHttpRequests { auth ->
                // ── Admin-only endpoints (must come before catch-all GET rule) ──
                auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/status").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.POST, "/api/gallery/refresh").hasRole("ADMIN")

                // ── Public endpoints (must come before parameterized patterns) ──
                auth.requestMatchers("/api/auth/**").permitAll()

                // Public GET endpoints
                auth.requestMatchers(HttpMethod.GET, "/api/rsvp/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/payments/fees").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/payments/angels").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/payments/summary/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/payments/summary").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/gallery/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/family-tree/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/meetings/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/ticket/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/capabilities").permitAll()

                // Public mutations (specific paths before parameterized patterns)
                auth.requestMatchers(HttpMethod.POST, "/api/rsvp").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/payments/checkout").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/events/*/register").permitAll()
                auth.requestMatchers(HttpMethod.DELETE, "/api/events/*/register/**").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/checkin/send").permitAll()

                // Admin check-in (after /api/checkin/send to avoid {token} matching "send")
                auth.requestMatchers(HttpMethod.POST, "/api/checkin/{token}").hasRole("ADMIN")

                // ── All other API requests require ADMIN ──
                auth.requestMatchers("/api/**").hasRole("ADMIN")

                // Static resources are public
                auth.anyRequest().permitAll()
            }
            .addFilterBefore(googleAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
