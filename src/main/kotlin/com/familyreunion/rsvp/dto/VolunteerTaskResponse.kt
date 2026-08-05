package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class VolunteerTaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val eventId: Long,
    val eventTitle: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val eventDateTime: LocalDateTime,
    val signups: List<VolunteerSignupDto>,
    val signupCount: Int
)
