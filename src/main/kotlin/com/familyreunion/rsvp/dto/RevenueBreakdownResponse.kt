package com.familyreunion.rsvp.dto

import java.math.BigDecimal

/**
 * Completed income split by what it was for, so full-fee revenue can be read apart from
 * pay-what-you-can donations. Dollars, like every other payment response.
 *
 * [total] is the sum of the four and therefore includes [angel], even though Angel money is
 * deliberately excluded from per-branch balances — this is the books, not a branch's balance.
 */
data class RevenueBreakdownResponse(
    val fees: BigDecimal,
    val shirts: BigDecimal,
    val donations: BigDecimal,
    val angel: BigDecimal,
    val total: BigDecimal
)
