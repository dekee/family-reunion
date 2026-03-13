package com.familyreunion.rsvp.dto

import java.math.BigDecimal

data class PaymentDetailResponse(
    val id: Long,
    val rsvpId: Long,
    val familyName: String,
    val amount: BigDecimal,
    val status: String,
    val createdAt: String,
    val payerName: String?,
    val payerEmail: String?,
    val checkinToken: String?,
    val checkedIn: Boolean,
    val checkedInAt: String?,
    val lineItems: List<LineItemResponse>
)

data class LineItemResponse(
    val name: String,
    val ageGroup: String,
    val amount: BigDecimal,
    val isGuest: Boolean
)
