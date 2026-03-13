package com.familyreunion.rsvp.dto

import java.math.BigDecimal

data class AngelContributorResponse(
    val payerName: String,
    val familyName: String,
    val amount: BigDecimal,
    val date: String
)
