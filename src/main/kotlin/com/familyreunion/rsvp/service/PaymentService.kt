package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.config.FeeConfig
import com.familyreunion.rsvp.config.StripeConfig
import com.familyreunion.rsvp.dto.AngelContributorResponse
import com.familyreunion.rsvp.dto.CheckoutRequest
import com.familyreunion.rsvp.dto.LineItemResponse
import com.familyreunion.rsvp.dto.PaidGuestInfo
import com.familyreunion.rsvp.dto.PaymentDetailResponse
import com.familyreunion.rsvp.dto.PaymentResponse
import com.familyreunion.rsvp.dto.PaymentSummaryResponse
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.Rsvp
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
    private val feeConfig: FeeConfig
) {

    fun createCheckoutSession(request: CheckoutRequest): String {
        if (!stripeConfig.isConfigured()) {
            throw IllegalStateException("Stripe is not configured. Please set STRIPE_SECRET_KEY.")
        }

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

        var calculatedAmountCents = 0L
        for (memberId in memberIds) {
            val member = familyMembers[memberId] ?: continue
            calculatedAmountCents += feeForAgeGroup(member.ageGroup)
        }
        for (guest in guests) {
            val ageGroup = try { AgeGroup.valueOf(guest.ageGroup) } catch (_: Exception) { AgeGroup.ADULT }
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

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrlWithRsvp)
            .setCancelUrl(cancelUrlWithRsvp)
            .putMetadata("rsvpId", rsvpId.toString())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(calculatedAmountCents)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Tumblin Family Reunion – ${rsvp.familyName} Family")
                                    .setDescription("Reunion fee payment")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val session = Session.create(params)

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
                amount = BigDecimal.valueOf(fee).divide(BigDecimal(100))
            )
            payment.lineItems.add(lineItem)
        }

        for (guest in guests) {
            val ageGroup = try { AgeGroup.valueOf(guest.ageGroup) } catch (_: Exception) { AgeGroup.ADULT }
            val fee = feeForAgeGroup(ageGroup)
            val lineItem = PaymentLineItem(
                payment = payment,
                guestName = guest.name,
                ageGroup = ageGroup,
                amount = BigDecimal.valueOf(fee).divide(BigDecimal(100))
            )
            payment.lineItems.add(lineItem)
        }

        if (angelAmount > 0) {
            val angelLineItem = PaymentLineItem(
                payment = payment,
                guestName = "Angel Contribution",
                ageGroup = AgeGroup.ADULT,
                amount = BigDecimal.valueOf(angelAmount).divide(BigDecimal(100))
            )
            payment.lineItems.add(angelLineItem)
        }

        if (payment.lineItems.isNotEmpty()) {
            paymentRepository.save(payment)
        }

        return session.url
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
        val totalPaid = completedPayments
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
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

        // Collect paid member IDs and guests from completed payment line items
        val completedPaymentIds = completedPayments.map { it.id }
        val lineItems = if (completedPaymentIds.isNotEmpty()) {
            paymentLineItemRepository.findByCompletedPaymentIds(completedPaymentIds, PaymentStatus.COMPLETED)
        } else emptyList()

        val paidMemberIds = lineItems.filter { it.familyMemberId != null }.map { it.familyMemberId!! }.distinct()
        val paidGuests = lineItems.filter { it.guestName != null }.map {
            PaidGuestInfo(name = it.guestName!!, ageGroup = it.ageGroup.name, amount = it.amount)
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
            paidGuests = paidGuests
        )
    }

    fun calculateAmountOwed(rsvp: Rsvp): BigDecimal {
        var totalCents = 0L
        for (attendee in rsvp.attendees) {
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
                familyName = rsvp?.familyName ?: "Unknown",
                amount = payment.amount,
                status = payment.status.name,
                createdAt = payment.createdAt.toString(),
                payerName = payment.payerName,
                payerEmail = payment.payerEmail,
                checkinToken = if (payment.status == PaymentStatus.COMPLETED) payment.checkinToken else null,
                checkedIn = payment.checkedIn,
                checkedInAt = payment.checkedInAt?.toString(),
                lineItems = lineItems.map { li ->
                    LineItemResponse(
                        name = li.familyMemberName ?: li.guestName ?: "Unknown",
                        ageGroup = li.ageGroup.name,
                        amount = li.amount,
                        isGuest = li.guestName != null
                    )
                }
            )
        }.sortedByDescending { it.createdAt }
    }

    @Transactional(readOnly = true)
    fun getAngelContributors(): List<AngelContributorResponse> {
        val completedPayments = paymentRepository.findAll().filter { it.status == PaymentStatus.COMPLETED }
        val angels = mutableListOf<AngelContributorResponse>()
        for (payment in completedPayments) {
            val lineItems = paymentLineItemRepository.findByPaymentId(payment.id)
            val angelItem = lineItems.find { it.guestName == "Angel Contribution" }
            if (angelItem != null) {
                angels.add(AngelContributorResponse(
                    payerName = payment.payerName ?: "Anonymous",
                    familyName = payment.rsvp?.familyName ?: "Unknown",
                    amount = angelItem.amount,
                    date = payment.createdAt.toLocalDate().toString()
                ))
            }
        }
        return angels.sortedByDescending { it.date }
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
