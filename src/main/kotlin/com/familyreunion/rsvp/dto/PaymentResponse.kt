package com.familyreunion.rsvp.dto

import java.math.BigDecimal

data class PaymentResponse(
    val id: Long,
    val rsvpId: Long,
    val familyName: String,
    val amount: BigDecimal,
    val status: String,
    val createdAt: String,
    val checkinToken: String? = null
)
