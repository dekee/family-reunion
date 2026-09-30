package com.familyreunion.rsvp.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * A pay-what-you-can checkout for family members who have not paid their fee.
 *
 * Half donor-authoritative, half recomputed: [donationCents] is whatever the donor chose (clamped,
 * like an Angel gift), while the T-shirt portion is priced server-side from `app.fees.shirt` and the
 * total is equality-checked against [amount] exactly as the fee path is. So a tampered shirt price is
 * rejected, but a generous or meagre donation is accepted as given.
 *
 * Bounds live in the service rather than as `@Min`/`@Max` here for the same reason
 * [DonationCheckoutRequest] does it: bean-validation failures serialize as `{"errors": {...}}`, which
 * the frontend does not unwrap, while IllegalArgumentException becomes `{"error": "..."}` and is shown
 * to the donor verbatim.
 */
data class ContributionCheckoutRequest(
    @field:NotNull
    val rsvpId: Long = 0,

    /** Total in cents, client-computed. Must equal donationCents + (shirt price x shirts). */
    @field:Min(100)
    val amount: Long = 0,

    /** The freeform gift portion. May be 0 when someone is only buying shirts. */
    val donationCents: Long = 0,

    val attendees: List<ContributionAttendee> = emptyList()
)

/**
 * One person this donation covers. They are attending either way — [wantsShirt] only decides whether
 * they also get a shirt, and therefore whether their line item costs anything.
 *
 * Exactly one of [memberId] and [guestName] must be set: a member is someone on the family tree,
 * named and aged from their own record, so [ageGroup] is ignored for them; a guest is anyone else,
 * named and aged from this request like the fee page's guests.
 */
data class ContributionAttendee(
    val memberId: Long? = null,
    val guestName: String? = null,
    /** Guests only — a member's age group comes from their family-tree record, never the client. */
    val ageGroup: String = "ADULT",
    val wantsShirt: Boolean = false,
    val tshirtSize: String? = null
)
