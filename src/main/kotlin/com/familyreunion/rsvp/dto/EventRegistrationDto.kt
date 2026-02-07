package com.familyreunion.rsvp.dto

data class EventRegistrationDto(
    val id: Long,
    val familyMemberId: Long,
    val familyMemberName: String
)
