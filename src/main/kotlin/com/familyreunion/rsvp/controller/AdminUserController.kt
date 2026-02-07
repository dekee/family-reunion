package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.AdminUserRequest
import com.familyreunion.rsvp.dto.AdminUserResponse
import com.familyreunion.rsvp.model.AdminUser
import com.familyreunion.rsvp.repository.AdminUserRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val adminUserRepository: AdminUserRepository
) {

    @GetMapping
    fun getAll(): ResponseEntity<List<AdminUserResponse>> {
        val users = adminUserRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(users)
    }

    @PostMapping
    fun create(@Valid @RequestBody request: AdminUserRequest): ResponseEntity<AdminUserResponse> {
        if (adminUserRepository.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        val user = adminUserRepository.save(
            AdminUser(email = request.email, name = request.name)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        if (!adminUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        adminUserRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    private fun AdminUser.toResponse() = AdminUserResponse(
        id = id,
        email = email,
        name = name,
        createdAt = createdAt.toString()
    )
}
