package com.familyreunion.rsvp.dto

data class RsvpSummaryResponse(
    val totalFamilies: Int,
    val totalHeadcount: Int,
    val adultCount: Int,
    val childCount: Int,
    val infantCount: Int,
    val lodgingCount: Int
)
