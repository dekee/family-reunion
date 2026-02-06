package com.familyreunion.rsvp.dto

import com.familyreunion.rsvp.model.AgeGroup

data class AttendeeDto(
    val id: Long? = null,
    val familyMemberId: Long? = null,
    val familyMemberName: String? = null,
    val familyMemberAgeGroup: AgeGroup? = null,
    val familyMemberParentName: String? = null,
    val guestName: String? = null,
    val guestAgeGroup: AgeGroup? = null,
    val dietaryNeeds: String? = null
)
