package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/reset")
class AdminResetController(
    private val paymentService: PaymentService
) {

    @DeleteMapping("/payments")
    fun resetPayments(): ResponseEntity<Map<String, Any>> {
        val count = paymentService.resetAllPayments()
        return ResponseEntity.ok(mapOf("deleted" to count, "message" to "All payments and line items deleted"))
    }

    @DeleteMapping("/checkins")
    fun resetCheckins(): ResponseEntity<Map<String, Any>> {
        val count = paymentService.resetAllCheckins()
        return ResponseEntity.ok(mapOf("reset" to count, "message" to "All check-ins cleared"))
    }
}
