package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.config.FeeConfig
import com.familyreunion.rsvp.dto.AngelContributorResponse
import com.familyreunion.rsvp.dto.CheckoutRequest
import com.familyreunion.rsvp.dto.DonationCheckoutRequest
import com.familyreunion.rsvp.dto.LineItemSizeResponse
import com.familyreunion.rsvp.dto.PaymentDetailResponse
import com.familyreunion.rsvp.dto.PaymentSummaryResponse
import com.familyreunion.rsvp.dto.UpdateLineItemSizeRequest
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.PaymentService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val feeConfig: FeeConfig,
    private val rateLimiter: IpRateLimiter
) {

    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    @GetMapping("/fees")
    fun getFees(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf(
            "ADULT" to feeConfig.adult,
            "SPOUSE" to feeConfig.spouse,
            "CHILD" to feeConfig.child,
            "INFANT" to feeConfig.infant
        ))
    }

    @PostMapping("/checkout")
    fun createCheckout(@Valid @RequestBody request: CheckoutRequest): ResponseEntity<Map<String, String>> {
        val url = paymentService.createCheckoutSession(request)
        return ResponseEntity.ok(mapOf("url" to url))
    }

    /**
     * Public: a standalone Angel Fund gift, with no RSVP and no fees. Rate-limited because this is
     * the only unauthenticated endpoint that creates Stripe objects. Returns the error as a plain
     * ResponseEntity rather than throwing ResponseStatusException, whose default body omits
     * `message` and would reach the donor as a bare "Too Many Requests".
     */
    @PostMapping("/donate")
    fun createDonationCheckout(
        @Valid @RequestBody request: DonationCheckoutRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("donate:$ip", maxRequests = 5, windowSeconds = 600)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "Too many donation attempts. Please try again in a few minutes."))
        }
        val url = paymentService.createDonationCheckoutSession(request)
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

    @GetMapping("/history")
    fun getPaymentHistory(): ResponseEntity<List<PaymentDetailResponse>> {
        return ResponseEntity.ok(paymentService.getPaymentHistory())
    }

    /** Public: lets a paid attendee pick/change their T-shirt size from the pay page. */
    @PutMapping("/line-items/{id}/size")
    fun updateLineItemSize(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLineItemSizeRequest
    ): ResponseEntity<LineItemSizeResponse> {
        return ResponseEntity.ok(paymentService.updateLineItemSize(id, request))
    }

    @GetMapping("/angels")
    fun getAngelContributors(): ResponseEntity<List<AngelContributorResponse>> {
        return ResponseEntity.ok(paymentService.getAngelContributors())
    }
}
