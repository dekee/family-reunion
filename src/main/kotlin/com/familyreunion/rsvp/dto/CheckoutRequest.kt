package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CheckoutRequest(
    @field:NotNull
    val rsvpId: Long = 0,

    @field:Min(100)
    val amount: Long = 0
)
