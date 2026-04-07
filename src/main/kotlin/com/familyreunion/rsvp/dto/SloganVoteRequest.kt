package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotBlank

data class SloganVoteRequest(
    val sloganId: Long = 0,
    @field:NotBlank val voterName: String = ""
)
