package com.familyreunion.rsvp.dto

/**
 * An order for Sister Dance Off shirts. Anyone with the link may order; there is no RSVP.
 *
 * The price is the server's: [totalCents] is client-computed and must equal shirt count x
 * `app.fees.dance-off-shirt` exactly, as on the fee path. Bounds are checked in the service, not with
 * bean-validation annotations, so failures come back as `{"error": "..."}` and are shown verbatim
 * (the same reason [ContributionCheckoutRequest] does it).
 */
data class DanceOffShirtCheckoutRequest(
    /** Who is paying and picking up. Shown to admins; also the default name on each shirt. */
    val buyerName: String? = null,
    val totalCents: Long = 0,
    val items: List<DanceOffShirtItem> = emptyList()
)

/** One size, how many of it, and optionally who it is for. */
data class DanceOffShirtItem(
    val tshirtSize: String? = null,
    val quantity: Int = 1,
    val wearerName: String? = null
)
