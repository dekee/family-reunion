package com.familyreunion.rsvp.dto

import java.math.BigDecimal

data class TicketResponse(
    val checkinToken: String,
    val familyName: String,
    val payerName: String,
    val payerEmail: String? = null,
    val amount: BigDecimal,
    val checkedIn: Boolean,
    val checkedInAt: String? = null,
    val attendees: List<TicketAttendee>
)

data class TicketAttendee(
    val name: String,
    val ageGroup: String,
    val isGuest: Boolean
)

data class CheckinResponse(
    val success: Boolean,
    val message: String,
    val ticket: TicketResponse? = null
)

data class CheckinStatusResponse(
    val total: Int,
    val checkedIn: Int,
    val tickets: List<TicketResponse>
)

data class SendTicketRequest(
    val checkinToken: String,
    val email: String? = null,
    val phone: String? = null
)
