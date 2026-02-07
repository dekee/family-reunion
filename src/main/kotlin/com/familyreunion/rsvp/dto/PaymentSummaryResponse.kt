package com.familyreunion.rsvp.dto

import java.math.BigDecimal

data class PaymentSummaryResponse(
    val rsvpId: Long,
    val familyName: String,
    val totalOwed: BigDecimal,
    val totalPaid: BigDecimal,
    val balance: BigDecimal,
    val status: String,
    val payments: List<PaymentResponse>
)
