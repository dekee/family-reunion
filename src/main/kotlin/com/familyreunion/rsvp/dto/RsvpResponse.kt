package com.familyreunion.rsvp.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class RsvpResponse(
    val id: Long,
    val familyName: String,
    val headOfHouseholdName: String,
    val email: String,
    val phone: String?,
    val familyMembers: List<FamilyMemberDto>,
    val needsLodging: Boolean,
    @JsonFormat(pattern = "yyyy-MM-dd") val arrivalDate: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd") val departureDate: LocalDate?,
    val notes: String?
)
