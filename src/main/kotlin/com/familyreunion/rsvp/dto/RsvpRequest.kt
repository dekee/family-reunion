package com.familyreunion.rsvp.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class RsvpRequest(
    @field:NotBlank val familyName: String = "",
    @field:NotBlank val headOfHouseholdName: String = "",
    @field:NotBlank @field:Email val email: String = "",
    val phone: String? = null,
    @field:NotEmpty @field:Valid val attendees: List<AttendeeDto> = emptyList(),
    val needsLodging: Boolean = false,
    @JsonFormat(pattern = "yyyy-MM-dd") val arrivalDate: LocalDate? = null,
    @JsonFormat(pattern = "yyyy-MM-dd") val departureDate: LocalDate? = null,
    val notes: String? = null
)
