package com.familyreunion.rsvp.dto

data class PaidGuestInfo(
    val name: String,
    val ageGroup: String,
    val amount: java.math.BigDecimal,
    val lineItemId: Long,
    val tshirtSize: String?
)

data class PaidMemberInfo(
    val memberId: Long,
    val lineItemId: Long,
    val tshirtSize: String?
)

data class CheckoutGuestInfo(
    val name: String = "",
    val ageGroup: String = "ADULT",
    val fee: Long = 0,
    val tshirtSize: String? = null
)
