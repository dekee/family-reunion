package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AdminUserRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val name: String
)

data class AdminUserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val createdAt: String
)

data class AuthMeResponse(
    val email: String,
    val name: String,
    val isAdmin: Boolean
)
