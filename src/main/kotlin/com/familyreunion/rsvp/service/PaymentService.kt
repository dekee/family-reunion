package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.config.StripeConfig
import com.familyreunion.rsvp.dto.PaymentResponse
import com.familyreunion.rsvp.dto.PaymentSummaryResponse
import com.familyreunion.rsvp.exception.RsvpNotFoundException
import com.familyreunion.rsvp.model.AgeGroup
import com.familyreunion.rsvp.model.Payment
import com.familyreunion.rsvp.model.PaymentStatus
import com.familyreunion.rsvp.model.Rsvp
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
    private val rsvpRepository: RsvpRepository,
    private val stripeConfig: StripeConfig
) {
    companion object {
        const val ADULT_FEE = 10000L   // $100 in cents
        const val CHILD_FEE = 5000L    // $50 in cents
        const val SPOUSE_FEE = 10000L  // $100 in cents
        const val INFANT_FEE = 0L
    }

    fun createCheckoutSession(rsvpId: Long, amountCents: Long): String {
        if (!stripeConfig.isConfigured()) {
            throw IllegalStateException("Stripe is not configured. Please set STRIPE_SECRET_KEY.")
        }

        val rsvp = rsvpRepository.findById(rsvpId)
            .orElseThrow { RsvpNotFoundException(rsvpId) }

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(stripeConfig.successUrl)
            .setCancelUrl(stripeConfig.cancelUrl)
            .putMetadata("rsvpId", rsvpId.toString())
            .setCustomerEmail(rsvp.email)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(amountCents)
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
            amount = BigDecimal.valueOf(amountCents).divide(BigDecimal(100)),
            stripeSessionId = session.id,
            status = PaymentStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        paymentRepository.save(payment)

        return session.url
    }

    private val log = org.slf4j.LoggerFactory.getLogger(PaymentService::class.java)

    fun handleWebhook(payload: String, sigHeader: String) {
        val event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret())

        log.info("Webhook event type: ${event.type}, event id: ${event.id}")

        if (event.type == "checkout.session.completed") {
            // Try SDK deserialization first, fall back to raw JSON parsing
            var sessionId: String? = null
            var paymentIntentId: String? = null

            val session = event.dataObjectDeserializer.`object`.orElse(null) as? Session
            if (session != null) {
                sessionId = session.id
                paymentIntentId = session.paymentIntent
            } else {
                // Fall back: parse from raw JSON
                log.info("SDK deserialization failed, parsing raw JSON")
                val rawJson = event.dataObjectDeserializer.rawJson
                if (rawJson != null) {
                    val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                    val node = mapper.readTree(rawJson)
                    sessionId = node.get("id")?.asText()
                    paymentIntentId = node.get("payment_intent")?.asText()
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
        val totalPaid = payments
            .filter { it.status == PaymentStatus.COMPLETED }
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

        return PaymentSummaryResponse(
            rsvpId = rsvp.id,
            familyName = rsvp.familyName,
            totalOwed = totalOwed,
            totalPaid = totalPaid,
            balance = balance,
            status = status,
            payments = payments.map { toPaymentResponse(it, rsvp) }
        )
    }

    fun calculateAmountOwed(rsvp: Rsvp): BigDecimal {
        var totalCents = 0L
        for (attendee in rsvp.attendees) {
            totalCents += when (attendee.ageGroup) {
                AgeGroup.ADULT -> ADULT_FEE
                AgeGroup.SPOUSE -> SPOUSE_FEE
                AgeGroup.CHILD -> CHILD_FEE
                AgeGroup.INFANT -> INFANT_FEE
            }
        }
        return BigDecimal.valueOf(totalCents).divide(BigDecimal(100))
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
