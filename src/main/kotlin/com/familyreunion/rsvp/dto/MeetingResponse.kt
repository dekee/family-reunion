package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class MeetingResponse(
    val id: Long,
    val title: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") val meetingDateTime: LocalDateTime,
    val zoomLink: String,
    val phoneNumber: String?,
    val meetingId: String?,
    val passcode: String?,
    val notes: String?
)
