package com.familyreunion.rsvp.dto

data class PaidGuestInfo(
    val name: String,
    val ageGroup: String,
    val amount: java.math.BigDecimal
)

data class CheckoutGuestInfo(
    val name: String = "",
    val ageGroup: String = "ADULT",
    val fee: Long = 0
)
