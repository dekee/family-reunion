package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotEmpty

data class EventRegisterRequest(
    @field:NotEmpty val familyMemberIds: List<Long> = emptyList()
)
