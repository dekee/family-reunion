package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.CheckinResponse
import com.familyreunion.rsvp.dto.CheckinStatusResponse
import com.familyreunion.rsvp.dto.SendTicketRequest
import com.familyreunion.rsvp.dto.TicketResponse
import com.familyreunion.rsvp.service.CheckinService
import com.familyreunion.rsvp.service.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant

@RestController
@RequestMapping("/api/checkin")
class CheckinController(
    private val checkinService: CheckinService,
    private val notificationService: NotificationService
) {

    // Rate limit: max 3 sends per token per 10 minutes, max 10 lifetime sends per token
    private val sendRateLimit = ConcurrentHashMap<String, MutableList<Instant>>()
    private val rateLimitWindow = 600L // seconds
    private val rateLimitMax = 3
    private val lifetimeSendCount = ConcurrentHashMap<String, Int>()
    private val lifetimeSendMax = 10

    @GetMapping("/ticket/{token}")
    fun getTicket(@PathVariable token: String): ResponseEntity<TicketResponse> {
        return try {
            ResponseEntity.ok(checkinService.getTicket(token))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{token}")
    fun checkin(@PathVariable token: String): ResponseEntity<CheckinResponse> {
        return ResponseEntity.ok(checkinService.checkin(token))
    }

    @GetMapping("/status")
    fun getCheckinStatus(): ResponseEntity<CheckinStatusResponse> {
        return ResponseEntity.ok(checkinService.getCheckinStatus())
    }

    @PostMapping("/send")
    fun sendTicket(@RequestBody request: SendTicketRequest): ResponseEntity<Map<String, String>> {
        // Rate limiting per token (sliding window + lifetime cap)
        val now = Instant.now()
        val lifetime = lifetimeSendCount.getOrDefault(request.checkinToken, 0)
        if (lifetime >= lifetimeSendMax) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "Send limit reached for this ticket."))
        }
        val timestamps = sendRateLimit.getOrPut(request.checkinToken) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { it.isBefore(now.minusSeconds(rateLimitWindow)) }
            if (timestamps.size >= rateLimitMax) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(mapOf("error" to "Too many send requests. Please try again later."))
            }
            timestamps.add(now)
        }
        lifetimeSendCount[request.checkinToken] = lifetime + 1

        val ticket = try {
            checkinService.getTicket(request.checkinToken)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid ticket"))
        }

        return try {
            when {
                !request.email.isNullOrBlank() -> {
                    notificationService.sendTicketEmail(
                        to = request.email,
                        familyName = ticket.familyName,
                        attendeeNames = ticket.attendees.map { it.name },
                        checkinToken = request.checkinToken
                    )
                    ResponseEntity.ok(mapOf("message" to "Ticket sent to ${request.email}"))
                }
                !request.phone.isNullOrBlank() -> {
                    notificationService.sendTicketSms(
                        to = request.phone,
                        familyName = ticket.familyName,
                        checkinToken = request.checkinToken
                    )
                    ResponseEntity.ok(mapOf("message" to "Ticket sent to ${request.phone}"))
                }
                else -> ResponseEntity.badRequest().body(mapOf("error" to "Provide email or phone"))
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Service not configured")))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("error" to ("Failed to send: ${e.message}")))
        }
    }

    @GetMapping("/capabilities")
    fun getCapabilities(): ResponseEntity<Map<String, Boolean>> {
        return ResponseEntity.ok(mapOf(
            "email" to notificationService.isEmailConfigured(),
            "sms" to notificationService.isSmsConfigured()
        ))
    }
}
