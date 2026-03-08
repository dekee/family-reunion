package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class EventRequest(
    @field:NotBlank val title: String = "",
    val description: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val eventDateTime: LocalDateTime = LocalDateTime.now(),
    @field:NotBlank val address: String = "",
    val hostName: String? = null,
    val notes: String? = null
)
