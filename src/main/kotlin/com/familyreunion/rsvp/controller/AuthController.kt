package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.AuthMeResponse
import com.familyreunion.rsvp.repository.AdminUserRepository
import com.familyreunion.rsvp.security.GoogleTokenVerifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val adminUserRepository: AdminUserRepository
) {

    @GetMapping("/me")
    fun me(@RequestHeader("Authorization", required = false) authHeader: String?): ResponseEntity<AuthMeResponse> {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build()
        }
        val token = authHeader.substring(7)
        val userInfo = googleTokenVerifier.verify(token)
            ?: return ResponseEntity.status(401).build()

        val isAdmin = adminUserRepository.existsByEmail(userInfo.email)
        return ResponseEntity.ok(
            AuthMeResponse(
                email = userInfo.email,
                name = userInfo.name,
                isAdmin = isAdmin
            )
        )
    }
}
