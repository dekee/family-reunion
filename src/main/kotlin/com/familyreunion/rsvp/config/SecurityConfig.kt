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
                // Admin endpoints require ADMIN role (all methods including GET)
                auth.requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Auth endpoint is accessible to anyone (filter sets context if token valid)
                auth.requestMatchers("/api/auth/**").permitAll()

                // All GET requests are public
                auth.requestMatchers(HttpMethod.GET, "/**").permitAll()

                // RSVP: PUT (edit) is public, POST and DELETE are admin-only
                auth.requestMatchers(HttpMethod.PUT, "/api/rsvp/**").permitAll()

                // Event registration/unregistration is public
                auth.requestMatchers(HttpMethod.POST, "/api/events/*/register").permitAll()
                auth.requestMatchers(HttpMethod.DELETE, "/api/events/*/register/**").permitAll()

                // Payments are public
                auth.requestMatchers("/api/payments/**").permitAll()

                // Gallery: photos are public, refresh is admin-only
                auth.requestMatchers(HttpMethod.GET, "/api/gallery/**").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/gallery/refresh").hasRole("ADMIN")

                // Check-in: ticket lookup and sending are public, check-in action and status are admin
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/ticket/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/capabilities").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/checkin/send").permitAll()
                auth.requestMatchers(HttpMethod.POST, "/api/checkin/{token}").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.GET, "/api/checkin/status").hasRole("ADMIN")

                // All other POST/PUT/DELETE/PATCH require ADMIN role
                auth.requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")

                // Everything else (static resources, etc.) is public
                auth.anyRequest().permitAll()
            }
            .addFilterBefore(googleAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
