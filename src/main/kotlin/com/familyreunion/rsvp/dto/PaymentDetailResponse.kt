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
    /** True for a standalone Angel Fund gift: no RSVP, no attendees. */
    val donationOnly: Boolean = false,
    /** Admin-only view of the donor's chosen name, alongside Stripe's cardholder name. */
    val donorName: String? = null,
    val donorFamilyLabel: String? = null,
    val donorAnonymous: Boolean = false,
    val lineItems: List<LineItemResponse>
)

data class LineItemResponse(
    val name: String,
    val ageGroup: String,
    val amount: BigDecimal,
    val isGuest: Boolean,
    val lineItemId: Long,
    val tshirtSize: String?
)
