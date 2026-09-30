package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.config.FeeConfig
import com.familyreunion.rsvp.config.StripeConfig
import com.familyreunion.rsvp.dto.AngelContributorResponse
import com.familyreunion.rsvp.dto.ContributionCheckoutRequest
import com.familyreunion.rsvp.dto.CheckoutRequest
import com.familyreunion.rsvp.dto.DonationCheckoutRequest
import com.familyreunion.rsvp.dto.LineItemResponse
import com.familyreunion.rsvp.dto.LineItemSizeResponse
import com.familyreunion.rsvp.dto.PaidGuestInfo
import com.familyreunion.rsvp.dto.PaidMemberInfo
import com.familyreunion.rsvp.dto.PaymentDetailResponse
import com.familyreunion.rsvp.dto.PaymentResponse
import com.familyreunion.rsvp.dto.PaymentSummaryResponse
import com.familyreunion.rsvp.dto.RevenueBreakdownResponse
import com.familyreunion.rsvp.dto.UpdateLineItemSizeRequest
import com.familyreunion.rsvp.exception.LineItemNotFoundException
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.ANGEL_CONTRIBUTION_NAME
import com.familyreunion.rsvp.model.DONATION_LINE_ITEM_NAME
import com.familyreunion.rsvp.model.LineItemKind
import com.familyreunion.rsvp.model.NO_SHIRT_MESSAGE
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.Rsvp
import com.familyreunion.rsvp.model.TshirtSize
import com.familyreunion.rsvp.repository.FamilyMemberRepository
import com.familyreunion.rsvp.repository.PaymentLineItemRepository
import com.familyreunion.rsvp.repository.PaymentRepository
import com.familyreunion.rsvp.repository.RsvpRepository
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentLineItemRepository: PaymentLineItemRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val rsvpRepository: RsvpRepository,
    private val stripeConfig: StripeConfig,
    private val feeConfig: FeeConfig,
    private val notificationService: NotificationService
) {

    fun createCheckoutSession(request: CheckoutRequest): String {
        val rsvpId = request.rsvpId
        val memberIds = request.memberIds
        val guests = request.guests

        val rsvp = rsvpRepository.findById(rsvpId)
            .orElseThrow { RsvpNotFoundException(rsvpId) }

        // Validate that all member IDs belong to this RSVP's attendees
        val rsvpMemberIds = rsvp.attendees
            .mapNotNull { it.familyMember?.id }
            .toSet()
        val invalidIds = memberIds.filter { it !in rsvpMemberIds }
        if (invalidIds.isNotEmpty()) {
            throw IllegalArgumentException("Member IDs $invalidIds do not belong to RSVP $rsvpId")
        }

        // Calculate amount server-side from member age groups and guest age groups
        val familyMembers = if (memberIds.isNotEmpty()) {
            familyMemberRepository.findAllById(memberIds).associateBy { it.id }
        } else emptyMap()

        // Every person being paid for needs a T-shirt size (any size — age group is only a UI hint).
        // Validated before touching Stripe so bad input fails fast (and is testable without Stripe).
        val memberSizes: Map<Long, TshirtSize> = memberIds.associateWith { memberId ->
            val member = familyMembers[memberId]
                ?: throw IllegalArgumentException("Family member $memberId not found")
            TshirtSize.parse(request.memberSizes[memberId], member.name)
        }
        val guestAgeGroups: List<AgeGroup> = guests.map { guest ->
            try { AgeGroup.valueOf(guest.ageGroup) } catch (_: Exception) { AgeGroup.ADULT }
        }
        val guestSizes: List<TshirtSize> = guests.map { guest ->
            TshirtSize.parse(guest.tshirtSize, guest.name.ifBlank { "guest" })
        }

        if (!stripeConfig.isConfigured()) {
            throw IllegalStateException("Stripe is not configured. Please set STRIPE_SECRET_KEY.")
        }

        var calculatedAmountCents = 0L
        for (memberId in memberIds) {
            val member = familyMembers[memberId] ?: continue
            calculatedAmountCents += feeForAgeGroup(member.ageGroup)
        }
        for (ageGroup in guestAgeGroups) {
            calculatedAmountCents += feeForAgeGroup(ageGroup)
        }

        // Add angel contribution (custom donation amount)
        val angelAmount = request.angelAmount
        if (angelAmount > 0) {
            calculatedAmountCents += angelAmount
        }

        if (calculatedAmountCents <= 0 && memberIds.isNotEmpty()) {
            throw IllegalArgumentException("Calculated amount must be greater than zero")
        }

        // Verify client amount matches server calculation
        if (request.amount != calculatedAmountCents) {
            log.warn("Amount mismatch for rsvp $rsvpId: client sent ${request.amount}, server calculated $calculatedAmountCents")
            throw IllegalArgumentException(
                "Amount mismatch. Expected $calculatedAmountCents cents, got ${request.amount}. Please refresh and try again."
            )
        }

        val checkinToken = java.util.UUID.randomUUID().toString()
        val successUrlWithRsvp = "${stripeConfig.successUrl}&rsvpId=$rsvpId&token=$checkinToken"
        val cancelUrlWithRsvp = "${stripeConfig.cancelUrl}&rsvpId=$rsvpId"

        val session = createStripeSession(
            amountCents = calculatedAmountCents,
            productName = "Tumblin Family Reunion – ${rsvp.familyName} Family",
            description = "Reunion fee payment",
            successUrl = successUrlWithRsvp,
            cancelUrl = cancelUrlWithRsvp,
            metadata = mapOf("rsvpId" to rsvpId.toString())
        )

        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal.valueOf(calculatedAmountCents).divide(BigDecimal(100)),
            stripeSessionId = session.id,
            status = PaymentStatus.PENDING,
            createdAt = LocalDateTime.now(),
            checkinToken = checkinToken
        )
        paymentRepository.save(payment)

        // Save line items for member tracking
        for (memberId in memberIds) {
            val member = familyMembers[memberId] ?: continue
            val fee = feeForAgeGroup(member.ageGroup)
            val lineItem = PaymentLineItem(
                payment = payment,
                familyMemberId = memberId,
                familyMemberName = member.name,
                ageGroup = member.ageGroup,
                amount = BigDecimal.valueOf(fee).divide(BigDecimal(100)),
                tshirtSize = memberSizes[memberId],
                kind = LineItemKind.FEE
            )
            payment.lineItems.add(lineItem)
        }

        guests.forEachIndexed { index, guest ->
            val ageGroup = guestAgeGroups[index]
            val fee = feeForAgeGroup(ageGroup)
            val lineItem = PaymentLineItem(
                payment = payment,
                guestName = guest.name,
                ageGroup = ageGroup,
                amount = BigDecimal.valueOf(fee).divide(BigDecimal(100)),
                tshirtSize = guestSizes[index],
                kind = LineItemKind.FEE
            )
            payment.lineItems.add(lineItem)
        }

        if (angelAmount > 0) {
            payment.lineItems.add(angelLineItem(payment, angelAmount))
        }

        if (payment.lineItems.isNotEmpty()) {
            paymentRepository.save(payment)
        }

        return session.url
    }

    /**
     * Creates a standalone Angel Fund gift session: no RSVP, no family member, no fees.
     *
     * Deliberately separate from [createCheckoutSession]. That method's bulk is RSVP lookup,
     * member-ownership (IDOR) validation, T-shirt size parsing and fee recomputation, none of which
     * apply here — and its core anti-tamper control is *rejecting* any client/server amount
     * mismatch. For a gift the donor's amount is authoritative by design, so the control is
     * min/max clamping instead. Merging the two would express one as a special case of the other
     * and weaken the guarantee on the fee path.
     */
    fun createDonationCheckoutSession(request: DonationCheckoutRequest): String {
        val amountCents = request.amountCents

        // Validated before the Stripe guard so the whole validation surface is testable without a key.
        if (amountCents < MIN_DONATION_CENTS) {
            throw IllegalArgumentException("Minimum donation is $1.")
        }
        if (amountCents > MAX_DONATION_CENTS) {
            throw IllegalArgumentException(
                "Online gifts are capped at $10,000 — please contact an admin for a larger gift."
            )
        }

        if (!stripeConfig.isConfigured()) {
            throw IllegalStateException("Stripe is not configured. Please set STRIPE_SECRET_KEY.")
        }

        // Anonymous gifts store nothing identifying at all, rather than hiding it at render time.
        val anonymous = request.anonymous
        val donorName = if (anonymous) null else sanitizeDonorText(request.donorName)
        val familyLabel = if (anonymous) null else sanitizeDonorText(request.familyLabel)

        // Donor free text is kept out of Stripe product data and metadata: the only sinks for it
        // are our own DB, React (which escapes), and the admin email (which escapes explicitly).
        val session = createStripeSession(
            amountCents = amountCents,
            productName = "Tumblin Family Reunion – Angel Fund",
            description = "Angel Fund gift",
            successUrl = stripeConfig.donationSuccessUrl,
            cancelUrl = stripeConfig.donationCancelUrl,
            metadata = mapOf("kind" to "angel_donation")
        )

        val payment = Payment(
            rsvp = null,
            amount = BigDecimal.valueOf(amountCents).divide(BigDecimal(100)),
            stripeSessionId = session.id,
            status = PaymentStatus.PENDING,
            createdAt = LocalDateTime.now(),
            checkinToken = java.util.UUID.randomUUID().toString(),
            donorName = donorName,
            donorFamilyLabel = familyLabel,
            donorAnonymous = anonymous
        )
        paymentRepository.save(payment)

        // The angel line item is required, not cosmetic: getAngelContributors keys off it, and it
        // preserves the invariant that angel money is never counted as fee money.
        payment.lineItems.add(angelLineItem(payment, amountCents))
        paymentRepository.save(payment)

        return session.url
    }

    /**
     * Creates a pay-what-you-can session for members who have not paid their fee.
     *
     * Deliberately a third method rather than a flag on [createCheckoutSession]: that method's
     * guarantee is that the charge equals the age-group fees exactly, and threading an "except when
     * it doesn't" branch through it would weaken precisely the control it exists to provide. What is
     * shared is borrowed explicitly — the RSVP lookup, the IDOR guard, [createStripeSession].
     *
     * The split of authority: the donation is the donor's to choose (clamped, like an Angel gift),
     * the shirt portion is priced here and equality-checked (like a fee).
     */
    fun createContributionCheckoutSession(request: ContributionCheckoutRequest): String {
        val rsvpId = request.rsvpId
        val attendees = request.attendees

        val rsvp = rsvpRepository.findById(rsvpId)
            .orElseThrow { RsvpNotFoundException(rsvpId) }

        if (attendees.isEmpty()) {
            throw IllegalArgumentException("Select at least one person to give for.")
        }

        // An attendee is a family member or a guest, never both and never neither.
        val ambiguous = attendees.filter { (it.memberId == null) == (it.guestName == null) }
        if (ambiguous.isNotEmpty()) {
            throw IllegalArgumentException("Each person must be either a family member or a named guest.")
        }

        val memberIds = attendees.mapNotNull { it.memberId }
        val duplicates = memberIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw IllegalArgumentException("Member IDs $duplicates appear more than once")
        }

        // Same IDOR guard as the fee path: a member must belong to this RSVP.
        val rsvpMemberIds = rsvp.attendees
            .mapNotNull { it.familyMember?.id }
            .toSet()
        val invalidIds = memberIds.filter { it !in rsvpMemberIds }
        if (invalidIds.isNotEmpty()) {
            throw IllegalArgumentException("Member IDs $invalidIds do not belong to RSVP $rsvpId")
        }

        // The donations page hides people who are already covered, but hiding is not a control:
        // without this, a stale tab or a crafted request could buy a second shirt for someone who
        // has already been paid for. Keyed on COMPLETED only, matching how the pay page decides who
        // counts as paid — a PENDING session still does not reserve anyone.
        val completedPaymentIds = paymentRepository.findByRsvpId(rsvpId)
            .filter { it.status == PaymentStatus.COMPLETED }
            .map { it.id }
        val alreadyCovered = if (completedPaymentIds.isNotEmpty()) {
            paymentLineItemRepository
                .findByCompletedPaymentIds(completedPaymentIds, PaymentStatus.COMPLETED)
                .filter { it.isPerson }
                .mapNotNull { it.familyMemberId }
                .toSet()
        } else emptySet()
        val conflicts = memberIds.filter { it in alreadyCovered }
        if (conflicts.isNotEmpty()) {
            throw IllegalArgumentException(
                "Member IDs $conflicts have already been paid for. Please refresh and try again."
            )
        }

        val familyMembers = if (memberIds.isNotEmpty()) {
            familyMemberRepository.findAllById(memberIds).associateBy { it.id }
        } else emptyMap()

        // Resolve everyone to a name, an age group and an optional size up front. A member's name and
        // age come from their record; a guest's come from the request, so only a guest's are washed.
        // Only shirt buyers get a size — unlike the fee path, where everyone is getting one.
        val resolved: List<ResolvedAttendee> = attendees.map { attendee ->
            val memberId = attendee.memberId
            if (memberId != null) {
                val member = familyMembers[memberId]
                    ?: throw IllegalArgumentException("Family member $memberId not found")
                ResolvedAttendee(
                    memberId = memberId,
                    name = member.name,
                    ageGroup = member.ageGroup,
                    size = if (attendee.wantsShirt) TshirtSize.parse(attendee.tshirtSize, member.name) else null
                )
            } else {
                val guestName = sanitizeGuestName(attendee.guestName)
                val ageGroup = try {
                    AgeGroup.valueOf(attendee.ageGroup)
                } catch (_: Exception) {
                    AgeGroup.ADULT
                }
                ResolvedAttendee(
                    memberId = null,
                    name = guestName,
                    ageGroup = ageGroup,
                    size = if (attendee.wantsShirt) TshirtSize.parse(attendee.tshirtSize, guestName) else null
                )
            }
        }
        val shirtCount = resolved.count { it.size != null }

        val donationCents = request.donationCents
        if (donationCents < 0) {
            throw IllegalArgumentException("Donation cannot be negative.")
        }
        if (donationCents > MAX_DONATION_CENTS) {
            throw IllegalArgumentException(
                "Online gifts are capped at $10,000 — please contact an admin for a larger gift."
            )
        }

        val shirtPriceCents = feeConfig.shirt
        val calculatedAmountCents = donationCents + shirtCount * shirtPriceCents
        if (calculatedAmountCents < MIN_DONATION_CENTS) {
            throw IllegalArgumentException("Minimum donation is $1.")
        }

        // The shirt half is recomputed here, so a tampered shirt price is caught even though the
        // donation half is taken as given.
        if (request.amount != calculatedAmountCents) {
            log.warn("Contribution amount mismatch for rsvp $rsvpId: client sent ${request.amount}, server calculated $calculatedAmountCents")
            throw IllegalArgumentException(
                "Amount mismatch. Expected $calculatedAmountCents cents, got ${request.amount}. Please refresh and try again."
            )
        }

        // Last, so every rule above is testable without a Stripe key — as on the gift path.
        if (!stripeConfig.isConfigured()) {
            throw IllegalStateException("Stripe is not configured. Please set STRIPE_SECRET_KEY.")
        }

        val checkinToken = java.util.UUID.randomUUID().toString()

        val session = createStripeSession(
            amountCents = calculatedAmountCents,
            productName = "Tumblin Family Reunion – ${rsvp.familyName} Family",
            description = if (shirtCount == 0) "Reunion donation" else "Reunion donation + T-shirts",
            successUrl = "${stripeConfig.contributionSuccessUrl}&rsvpId=$rsvpId&token=$checkinToken",
            cancelUrl = "${stripeConfig.contributionCancelUrl}&rsvpId=$rsvpId",
            metadata = mapOf("rsvpId" to rsvpId.toString(), "kind" to "contribution")
        )

        val payment = Payment(
            rsvp = rsvp,
            amount = BigDecimal.valueOf(calculatedAmountCents).divide(BigDecimal(100)),
            stripeSessionId = session.id,
            status = PaymentStatus.PENDING,
            createdAt = LocalDateTime.now(),
            checkinToken = checkinToken
        )
        paymentRepository.save(payment)

        for (person in resolved) {
            payment.lineItems.add(
                PaymentLineItem(
                    payment = payment,
                    // Which column holds the name is what makes a row a member or a guest
                    // downstream (paidMembers vs paidGuests, and the isGuest flag on the ticket).
                    familyMemberId = person.memberId,
                    familyMemberName = if (person.memberId != null) person.name else null,
                    guestName = if (person.memberId == null) person.name else null,
                    ageGroup = person.ageGroup,
                    // A no-shirt attendee costs nothing; the donation row carries the money. The $0
                    // row still has to exist — it is what admits them and marks them as covered.
                    amount = if (person.size != null) {
                        BigDecimal.valueOf(shirtPriceCents).divide(BigDecimal(100))
                    } else BigDecimal.ZERO,
                    tshirtSize = person.size,
                    kind = if (person.size != null) LineItemKind.SHIRT else LineItemKind.ATTENDEE
                )
            )
        }

        if (donationCents > 0) {
            payment.lineItems.add(
                PaymentLineItem(
                    payment = payment,
                    guestName = DONATION_LINE_ITEM_NAME,
                    ageGroup = AgeGroup.ADULT,
                    amount = BigDecimal.valueOf(donationCents).divide(BigDecimal(100)),
                    kind = LineItemKind.DONATION
                )
            )
        }

        paymentRepository.save(payment)

        return session.url
    }

    private fun angelLineItem(payment: Payment, amountCents: Long) = PaymentLineItem(
        payment = payment,
        guestName = ANGEL_CONTRIBUTION_NAME,
        ageGroup = AgeGroup.ADULT,
        amount = BigDecimal.valueOf(amountCents).divide(BigDecimal(100)),
        kind = LineItemKind.ANGEL
    )

    /** One attendee with everything resolved: members from their record, guests from the request. */
    private data class ResolvedAttendee(
        val memberId: Long?,
        val name: String,
        val ageGroup: AgeGroup,
        val size: TshirtSize?
    )

    /**
     * Guest names arrive from an unauthenticated form and end up on the ticket, the admin page and
     * the T-shirt list, so they get the same wash as donor text.
     *
     * The reserved-name check is not cosmetic: [PaymentLineItem.isAngel] still recognises a pre-V9
     * angel row by its name, so a guest called "Angel Contribution" would be counted as a gift and
     * silently dropped from the ticket they just paid for.
     */
    private fun sanitizeGuestName(raw: String?): String {
        val clean = sanitizeDonorText(raw)
            ?: throw IllegalArgumentException("Every guest needs a name.")
        if (clean.equals(ANGEL_CONTRIBUTION_NAME, ignoreCase = true) ||
            clean.equals(DONATION_LINE_ITEM_NAME, ignoreCase = true)
        ) {
            throw IllegalArgumentException("\"$clean\" is a reserved name — please use the guest's real name.")
        }
        return clean
    }

    /** Last line of defence for donor free text before it reaches the public page and admin email. */
    private fun sanitizeDonorText(raw: String?): String? = raw
        ?.replace(Regex("\\p{Cntrl}"), "")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.take(MAX_DONOR_TEXT_LENGTH)
        ?.ifBlank { null }

    private fun createStripeSession(
        amountCents: Long,
        productName: String,
        description: String,
        successUrl: String,
        cancelUrl: String,
        metadata: Map<String, String>
    ): Session {
        val builder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(amountCents)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(productName)
                                    .setDescription(description)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        metadata.forEach { (key, value) -> builder.putMetadata(key, value) }
        return Session.create(builder.build())
    }

    fun feeForAgeGroup(ageGroup: AgeGroup): Long = feeConfig.feeForAgeGroup(ageGroup)

    private val log = org.slf4j.LoggerFactory.getLogger(PaymentService::class.java)

    fun handleWebhook(payload: String, sigHeader: String) {
        val event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret())

        log.info("Webhook event type: ${event.type}, event id: ${event.id}")

        if (event.type == "checkout.session.completed") {
            // Try SDK deserialization first, fall back to raw JSON parsing
            var sessionId: String? = null
            var paymentIntentId: String? = null
            var payerName: String? = null
            var payerEmail: String? = null

            val session = event.dataObjectDeserializer.`object`.orElse(null) as? Session
            if (session != null) {
                sessionId = session.id
                paymentIntentId = session.paymentIntent
                payerName = session.customerDetails?.name
                payerEmail = session.customerDetails?.email
            } else {
                // Fall back: parse from raw JSON
                log.info("SDK deserialization failed, parsing raw JSON")
                val rawJson = event.dataObjectDeserializer.rawJson
                if (rawJson != null) {
                    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                    val node = mapper.readTree(rawJson)
                    sessionId = node.get("id")?.asText()
                    paymentIntentId = node.get("payment_intent")?.asText()
                    val customerDetails = node.get("customer_details")
                    if (customerDetails != null) {
                        payerName = customerDetails.get("name")?.asText()
                        payerEmail = customerDetails.get("email")?.asText()
                    }
                }
            }

            if (sessionId == null) {
                log.warn("Could not extract session id from webhook event")
                return
            }

            log.info("Webhook session id: $sessionId, payment intent: $paymentIntentId")

            val existing = paymentRepository.findByStripeSessionId(sessionId)
            if (existing == null) {
                log.warn("No payment record found for session: $sessionId")
                return
            }

            if (existing.status == PaymentStatus.COMPLETED) {
                log.info("Payment ${existing.id} already completed, skipping")
                return
            }

            existing.status = PaymentStatus.COMPLETED
            existing.stripePaymentIntentId = paymentIntentId
            existing.payerName = payerName
            existing.payerEmail = payerEmail
            paymentRepository.save(existing)
            log.info("Payment ${existing.id} updated to COMPLETED for rsvp ${existing.rsvp?.id}")

            try {
                val lineItems = paymentLineItemRepository.findByPaymentId(existing.id)
                notificationService.sendPaymentNotificationToAdmins(
                    familyName = existing.rsvp?.familyName ?: "Angel Fund",
                    isDonation = existing.rsvp == null,
                    donorName = existing.donorName,
                    donorFamilyLabel = existing.donorFamilyLabel,
                    donorAnonymous = existing.donorAnonymous,
                    payerName = payerName,
                    payerEmail = payerEmail,
                    amount = existing.amount,
                    lineItems = lineItems,
                    timestamp = existing.createdAt
                )
            } catch (e: Exception) {
                log.error("Failed to send admin payment notification: ${e.message}", e)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getPaymentSummaries(): List<PaymentSummaryResponse> {
        val allRsvps = rsvpRepository.findAll()
        return allRsvps.map { rsvp -> buildSummary(rsvp) }
    }

    @Transactional(readOnly = true)
    fun getPaymentSummary(rsvpId: Long): PaymentSummaryResponse {
        val rsvp = rsvpRepository.findById(rsvpId)
            .orElseThrow { RsvpNotFoundException(rsvpId) }
        return buildSummary(rsvp)
    }

    private fun buildSummary(rsvp: Rsvp): PaymentSummaryResponse {
        val payments = paymentRepository.findByRsvpId(rsvp.id)
        val totalOwed = calculateAmountOwed(rsvp)
        val completedPayments = payments.filter { it.status == PaymentStatus.COMPLETED }

        // Collect paid member IDs and guests from completed payment line items
        val completedPaymentIds = completedPayments.map { it.id }
        val lineItems = if (completedPaymentIds.isNotEmpty()) {
            paymentLineItemRepository.findByCompletedPaymentIds(completedPaymentIds, PaymentStatus.COMPLETED)
        } else emptyList()

        // Angel contributions are donations — they must not count toward member fees owed
        val angelTotal = lineItems
            .filter { it.isAngel }
            .fold(BigDecimal.ZERO) { acc, li -> acc.add(li.amount) }

        val totalPaid = completedPayments
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
            .subtract(angelTotal)
        val totalPending = payments
            .filter { it.status == PaymentStatus.PENDING }
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
        val balance = totalOwed.subtract(totalPaid)

        val hasPending = totalPending > BigDecimal.ZERO
        val status = when {
            totalPaid >= totalOwed -> "PAID"
            totalPaid > BigDecimal.ZERO -> "PARTIAL"
            hasPending -> "PENDING"
            else -> "UNPAID"
        }

        // Angel gifts and the freeform portion of a donation checkout are money, not people —
        // never list them as paid attendees.
        val personItems = lineItems.filter { it.isPerson }
        val paidMemberItems = personItems.filter { it.familyMemberId != null }.distinctBy { it.familyMemberId }
        val paidMemberIds = paidMemberItems.map { it.familyMemberId!! }
        val paidMembers = paidMemberItems.map {
            PaidMemberInfo(memberId = it.familyMemberId!!, lineItemId = it.id, tshirtSize = it.tshirtSize?.name)
        }
        val paidGuests = personItems.filter { it.guestName != null }.map {
            PaidGuestInfo(
                name = it.guestName!!,
                ageGroup = it.ageGroup.name,
                amount = it.amount,
                lineItemId = it.id,
                tshirtSize = it.tshirtSize?.name
            )
        }

        return PaymentSummaryResponse(
            rsvpId = rsvp.id,
            familyName = rsvp.familyName,
            totalOwed = totalOwed,
            totalPaid = totalPaid,
            balance = balance,
            status = status,
            payments = payments.map { toPaymentResponse(it, rsvp) },
            paidMemberIds = paidMemberIds,
            paidGuests = paidGuests,
            paidMembers = paidMembers
        )
    }

    fun calculateAmountOwed(rsvp: Rsvp): BigDecimal {
        var totalCents = 0L
        for (attendee in rsvp.attendees) {
            // A member excluded from the RSVP is hidden from the pay page and can never be paid
            // for, so billing for them leaves a balance nobody can clear. Their attendee row is
            // left in place — this is the one place the exclusion has to be honoured.
            if (attendee.familyMember?.excludeFromRsvp == true) continue
            totalCents += feeForAgeGroup(attendee.ageGroup)
        }
        return BigDecimal.valueOf(totalCents).divide(BigDecimal(100))
    }

    fun resetAllPayments(): Int {
        val count = paymentRepository.count().toInt()
        paymentLineItemRepository.deleteAll()
        paymentRepository.deleteAll()
        return count
    }

    fun resetAllCheckins(): Int {
        val payments = paymentRepository.findAll().filter { it.checkedIn }
        payments.forEach {
            it.checkedIn = false
            it.checkedInAt = null
            paymentRepository.save(it)
        }
        return payments.size
    }

    @Transactional(readOnly = true)
    fun getPaymentHistory(): List<PaymentDetailResponse> {
        val allPayments = paymentRepository.findAll()
        return allPayments.map { payment ->
            val rsvp = payment.rsvp
            val lineItems = paymentLineItemRepository.findByPaymentId(payment.id)
            PaymentDetailResponse(
                id = payment.id,
                rsvpId = rsvp?.id ?: 0,
                familyName = rsvp?.familyName ?: "Angel Fund",
                amount = payment.amount,
                status = payment.status.name,
                createdAt = payment.createdAt.toString(),
                payerName = payment.payerName,
                payerEmail = payment.payerEmail,
                checkinToken = if (payment.status == PaymentStatus.COMPLETED) payment.checkinToken else null,
                checkedIn = payment.checkedIn,
                checkedInAt = payment.checkedInAt?.toString(),
                donationOnly = rsvp == null,
                donorName = payment.donorName,
                donorFamilyLabel = payment.donorFamilyLabel,
                donorAnonymous = payment.donorAnonymous,
                lineItems = lineItems.map { li ->
                    LineItemResponse(
                        name = li.displayName,
                        ageGroup = li.ageGroup.name,
                        amount = li.amount,
                        isGuest = li.guestName != null,
                        lineItemId = li.id,
                        tshirtSize = li.tshirtSize?.name,
                        kind = li.kind.name
                    )
                }
            )
        }.sortedByDescending { it.createdAt }
    }

    /**
     * Public pay-page edit of a paid person's T-shirt size. The caller proves it is looking at the
     * right family by supplying the (public) rsvpId; a mismatch is reported as not-found so the
     * endpoint cannot be used to probe which line item ids exist.
     */
    fun updateLineItemSize(lineItemId: Long, request: UpdateLineItemSizeRequest): LineItemSizeResponse {
        val lineItem = paymentLineItemRepository.findById(lineItemId)
            .orElseThrow { LineItemNotFoundException(lineItemId) }
        val payment = lineItem.payment ?: throw LineItemNotFoundException(lineItemId)
        if (payment.rsvp?.id != request.rsvpId) {
            throw LineItemNotFoundException(lineItemId)
        }
        if (payment.status != PaymentStatus.COMPLETED) {
            throw IllegalArgumentException("Payment not completed")
        }
        // Gated on hasShirt rather than just "not an angel row": a donation checkout can admit
        // someone without buying them a shirt, and that $0 row must not become a free shirt here.
        if (!lineItem.hasShirt) {
            throw IllegalArgumentException(NO_SHIRT_MESSAGE)
        }
        lineItem.tshirtSize = TshirtSize.parse(request.tshirtSize, lineItem.displayName)
        paymentLineItemRepository.save(lineItem)
        return LineItemSizeResponse(lineItemId = lineItem.id, tshirtSize = lineItem.tshirtSize!!.name)
    }

    /**
     * Completed income split by what it paid for. Admin-only: it is the books, not a branch balance,
     * so unlike [buildSummary] it does not exclude Angel money — it reports it as its own bucket.
     */
    @Transactional(readOnly = true)
    fun getRevenueBreakdown(): RevenueBreakdownResponse {
        val completedPaymentIds = paymentRepository.findAll()
            .filter { it.status == PaymentStatus.COMPLETED }
            .map { it.id }
        val lineItems = if (completedPaymentIds.isNotEmpty()) {
            paymentLineItemRepository.findByCompletedPaymentIds(completedPaymentIds, PaymentStatus.COMPLETED)
        } else emptyList()

        fun total(predicate: (PaymentLineItem) -> Boolean) = lineItems
            .filter(predicate)
            .fold(BigDecimal.ZERO) { acc, li -> acc.add(li.amount) }

        // isAngel is checked first everywhere below: a pre-V9 angel row defaults to kind = FEE and
        // is recognised only by its name sentinel, and must not be counted as fee revenue.
        val fees = total { !it.isAngel && it.kind == LineItemKind.FEE }
        val shirts = total { !it.isAngel && it.kind == LineItemKind.SHIRT }
        val donations = total { !it.isAngel && it.kind == LineItemKind.DONATION }
        val angel = total { it.isAngel }

        return RevenueBreakdownResponse(
            fees = fees,
            shirts = shirts,
            donations = donations,
            angel = angel,
            total = fees.add(shirts).add(donations).add(angel)
        )
    }

    @Transactional(readOnly = true)
    fun getAngelContributors(): List<AngelContributorResponse> {
        val completedPayments = paymentRepository.findAll().filter { it.status == PaymentStatus.COMPLETED }
        val angels = mutableListOf<AngelContributorResponse>()
        for (payment in completedPayments) {
            if (payment.donorHidden) continue
            val lineItems = paymentLineItemRepository.findByPaymentId(payment.id)
            // Sum every angel row rather than taking the first, so a payment carrying more than
            // one gift is reported in full.
            val angelItems = lineItems.filter { it.isAngel }
            if (angelItems.isNotEmpty()) {
                // donorAnonymous is checked before payerName: the webhook writes the Stripe
                // cardholder name there, which an anonymous donor must not have published.
                val displayName = when {
                    payment.donorAnonymous -> "Anonymous"
                    !payment.donorName.isNullOrBlank() -> payment.donorName!!
                    !payment.payerName.isNullOrBlank() -> payment.payerName!!
                    else -> "Anonymous"
                }
                // Empty string means "no family label" — a standalone gift has no branch to borrow
                // one from. The frontend omits the line entirely rather than printing " Family".
                val familyLabel = when {
                    payment.donorAnonymous -> ""
                    !payment.donorFamilyLabel.isNullOrBlank() -> payment.donorFamilyLabel!!
                    else -> payment.rsvp?.familyName ?: ""
                }
                angels.add(AngelContributorResponse(
                    payerName = displayName,
                    familyName = familyLabel,
                    amount = angelItems.fold(BigDecimal.ZERO) { acc, li -> acc.add(li.amount) },
                    date = payment.createdAt.toLocalDate().toString()
                ))
            }
        }
        return angels.sortedByDescending { it.date }
    }

    companion object {
        const val MIN_DONATION_CENTS = 100L         // $1
        const val MAX_DONATION_CENTS = 1_000_000L   // $10,000
        private const val MAX_DONOR_TEXT_LENGTH = 80
    }

    private fun toPaymentResponse(payment: Payment, rsvp: Rsvp) = PaymentResponse(
        id = payment.id,
        rsvpId = rsvp.id,
        familyName = rsvp.familyName,
        amount = payment.amount,
        status = payment.status.name,
        createdAt = payment.createdAt.toString()
    )
}
