package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class EventRequest(
    @field:NotBlank val title: String = "",
    val description: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val eventDateTime: LocalDateTime = LocalDateTime.now(),
    @field:NotBlank val address: String = "",
    val hostName: String? = null,
    val notes: String? = null,
    // Shown as a link on the public Events page, so only web addresses are allowed.
    @field:Size(max = 500)
    @field:Pattern(regexp = "^(\\s*|https?://\\S+)$", message = "must be a link starting with http:// or https://")
    val presentationUrl: String? = null
)
