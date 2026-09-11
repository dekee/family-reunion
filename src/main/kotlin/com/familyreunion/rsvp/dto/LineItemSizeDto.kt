package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateLineItemSizeRequest(
    @field:NotNull
    val rsvpId: Long = 0,

    @field:NotBlank
    val tshirtSize: String = ""
)

data class LineItemSizeResponse(
    val lineItemId: Long,
    val tshirtSize: String
)
