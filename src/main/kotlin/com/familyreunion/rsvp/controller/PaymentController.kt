package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.CheckoutRequest
import com.familyreunion.rsvp.dto.PaymentSummaryResponse
import com.familyreunion.rsvp.service.PaymentService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentController(private val paymentService: PaymentService) {

    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    @PostMapping("/checkout")
    fun createCheckout(@Valid @RequestBody request: CheckoutRequest): ResponseEntity<Map<String, String>> {
        val url = paymentService.createCheckoutSession(request)
        return ResponseEntity.ok(mapOf("url" to url))
    }

    @PostMapping("/webhook")
    fun handleWebhook(request: HttpServletRequest): ResponseEntity<String> {
        val payload = request.inputStream.bufferedReader().readText()
        val sigHeader = request.getHeader("Stripe-Signature")

        if (sigHeader.isNullOrBlank()) {
            log.warn("Webhook received without Stripe-Signature header")
            return ResponseEntity.badRequest().body("Missing Stripe-Signature header")
        }

        log.info("Stripe webhook received, signature present")
        return try {
            paymentService.handleWebhook(payload, sigHeader)
            log.info("Webhook processed successfully")
            ResponseEntity.ok("ok")
        } catch (e: Exception) {
            log.error("Webhook processing failed: ${e.message}", e)
            ResponseEntity.badRequest().body(e.message ?: "Webhook error")
        }
    }

    @GetMapping("/summary")
    fun getPaymentSummaries(): ResponseEntity<List<PaymentSummaryResponse>> {
        return ResponseEntity.ok(paymentService.getPaymentSummaries())
    }

    @GetMapping("/summary/{rsvpId}")
    fun getPaymentSummary(@PathVariable rsvpId: Long): ResponseEntity<PaymentSummaryResponse> {
        return ResponseEntity.ok(paymentService.getPaymentSummary(rsvpId))
    }
}
