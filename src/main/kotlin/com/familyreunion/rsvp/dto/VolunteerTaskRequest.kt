package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotBlank

data class VolunteerTaskRequest(
    @field:NotBlank val title: String = "",
    val description: String? = null,
    val eventId: Long = 0
)
