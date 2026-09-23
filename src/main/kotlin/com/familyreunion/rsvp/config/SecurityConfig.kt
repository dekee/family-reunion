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
                // Only the aggregate summary is public; full RSVP detail (names, emails,
                // phones, attendees, notes) requires ADMIN via the /api/** catch-all below.
                auth.requestMatchers(HttpMethod.GET, "/api/rsvp/summary").permitAll()
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
                auth.requestMatchers(HttpMethod.GET, "/api/slogans/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/designs/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/tributes/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/volunteer-tasks/**").permitAll()

                // Public mutations (specific paths before parameterized patterns)
                auth.requestMatchers(HttpMethod.POST, "/api/rsvp").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/payments/checkout").permitAll()
                // Standalone Angel Fund gift: public, rate-limited in the controller, amount
                // clamped server-side. Must stay above the /api/** catch-all — a fall-through
                // returns 403, and the frontend clears auth_token on 403, logging admins out.
                auth.requestMatchers(HttpMethod.POST, "/api/payments/donate").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/events/*/register").permitAll()
                auth.requestMatchers(HttpMethod.DELETE, "/api/events/*/register/**").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/checkin/send").permitAll()
                // T-shirt size edits: pay page uses the public rsvpId + lineItemId (same trust level as
                // checkout); ticket page is scoped by the checkin token. Both require a COMPLETED payment.
                auth.requestMatchers(HttpMethod.PUT, "/api/payments/line-items/*/size").permitAll()
                auth.requestMatchers(HttpMethod.PUT, "/api/checkin/ticket/*/sizes").permitAll()
                // Gallery upload is public at the HTTP layer; the controller enforces
                // the shared family upload password.
                auth.requestMatchers(HttpMethod.POST, "/api/gallery/upload").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/slogans/vote").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/designs/vote").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/tributes").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/volunteer-tasks/*/signup").permitAll()
                auth.requestMatchers(HttpMethod.DELETE, "/api/volunteer-tasks/*/signup/**").permitAll()

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
