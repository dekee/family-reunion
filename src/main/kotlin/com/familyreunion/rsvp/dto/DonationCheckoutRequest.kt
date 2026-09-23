package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.Size

/**
 * A standalone Angel Fund gift — no RSVP, no family member, no fees.
 *
 * The amount is bounds-checked in the service rather than with @Min/@Max on purpose: bean
 * validation failures are serialized as {"errors": {...}}, which the frontend's handleResponse
 * does not unwrap, so they would surface to a donor as the literal string "HTTP 400". A service
 * IllegalArgumentException becomes {"error": "..."} and is displayed verbatim.
 */
data class DonationCheckoutRequest(
    val amountCents: Long = 0,

    @field:Size(max = 80)
    val donorName: String? = null,

    @field:Size(max = 80)
    val familyLabel: String? = null,

    val anonymous: Boolean = false
)
