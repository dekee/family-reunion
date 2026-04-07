package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotBlank

data class SloganRequest(
    @field:NotBlank val slogan: String = ""
)
