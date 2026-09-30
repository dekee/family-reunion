package com.familyreunion.rsvp.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig(
    @Value("\${stripe.secret-key:}") private val secretKey: String,
    @Value("\${stripe.webhook-secret:}") private val webhookSecret: String,
    @Value("\${stripe.success-url:http://localhost:5173/pay?payment=success}") val successUrl: String,
    @Value("\${stripe.cancel-url:http://localhost:5173/pay?payment=cancelled}") val cancelUrl: String,
    // Standalone Angel Fund gifts return to the Thank You page, not the pay page. Used verbatim —
    // unlike the fee-checkout URLs, nothing is appended, since a gift has no rsvpId or ticket.
    @Value("\${stripe.donation-success-url:http://localhost:5173/thank-you?payment=success}") val donationSuccessUrl: String,
    @Value("\${stripe.donation-cancel-url:http://localhost:5173/thank-you?payment=cancelled}") val donationCancelUrl: String,
    // Donation checkouts return to the donations page. Like the fee-checkout URLs (and unlike the
    // Angel gift ones) rsvpId and the check-in token are appended, so the page can deep-link back to
    // the branch that was just given to.
    @Value("\${stripe.contribution-success-url:http://localhost:5173/donations?payment=success}") val contributionSuccessUrl: String,
    @Value("\${stripe.contribution-cancel-url:http://localhost:5173/donations?payment=cancelled}") val contributionCancelUrl: String
) {
    @PostConstruct
    fun init() {
        if (secretKey.isNotBlank()) {
            Stripe.apiKey = secretKey
        }
    }

    fun isConfigured(): Boolean = secretKey.isNotBlank()

    fun getWebhookSecret(): String = webhookSecret
}
