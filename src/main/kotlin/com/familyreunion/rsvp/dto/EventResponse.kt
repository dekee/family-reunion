package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class EventResponse(
    val id: Long,
    val title: String,
    val description: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val eventDateTime: LocalDateTime,
    val address: String,
    val hostName: String?,
    val notes: String?,
    val registrations: List<EventRegistrationDto>,
    val registrationCount: Int
)
