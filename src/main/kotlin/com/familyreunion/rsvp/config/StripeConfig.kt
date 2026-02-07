package com.familyreunion.rsvp.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig(
    @Value("\${stripe.secret-key:}") private val secretKey: String,
    @Value("\${stripe.webhook-secret:}") private val webhookSecret: String,
    @Value("\${stripe.success-url:http://localhost:5173/budget?payment=success}") val successUrl: String,
    @Value("\${stripe.cancel-url:http://localhost:5173/budget?payment=cancelled}") val cancelUrl: String
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
