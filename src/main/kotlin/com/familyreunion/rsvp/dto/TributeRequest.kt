package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TributeRequest(
    val siblingId: Long = 0,
    val authorId: Long = 0,
    @field:NotBlank @field:Size(max = 4000) val story: String = ""
)
