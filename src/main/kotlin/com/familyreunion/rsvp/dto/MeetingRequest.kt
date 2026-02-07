package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class MeetingRequest(
    @field:NotBlank val title: String = "",
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val meetingDateTime: LocalDateTime = LocalDateTime.now(),
    @field:NotBlank val zoomLink: String = "",
    val phoneNumber: String? = null,
    val meetingId: String? = null,
    val passcode: String? = null,
    val notes: String? = null
)
