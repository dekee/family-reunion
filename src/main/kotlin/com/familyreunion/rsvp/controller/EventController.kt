package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.EventRegisterRequest
import com.familyreunion.rsvp.dto.EventRequest
import com.familyreunion.rsvp.dto.EventResponse
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.EventService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService,
    private val rateLimiter: IpRateLimiter
) {

    @GetMapping
    fun getAllEvents(): ResponseEntity<List<EventResponse>> {
        return ResponseEntity.ok(eventService.getAllEvents())
    }

    @PostMapping
    fun createEvent(@Valid @RequestBody request: EventRequest): ResponseEntity<EventResponse> {
        val response = eventService.createEvent(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    fun updateEvent(
        @PathVariable id: Long,
        @Valid @RequestBody request: EventRequest
    ): ResponseEntity<EventResponse> {
        return ResponseEntity.ok(eventService.updateEvent(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteEvent(@PathVariable id: Long): ResponseEntity<Void> {
        eventService.deleteEvent(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/register")
    fun registerMembers(
        @PathVariable id: Long,
        @Valid @RequestBody request: EventRegisterRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EventResponse> {
        rateLimitOrThrow(httpRequest)
        return ResponseEntity.ok(eventService.registerMembers(id, request))
    }

    @DeleteMapping("/{id}/register/{memberId}")
    fun unregisterMember(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        rateLimitOrThrow(httpRequest)
        eventService.unregisterMember(id, memberId)
        return ResponseEntity.noContent().build()
    }

    // Public, unauthenticated registration endpoints: cap mutations per client IP to blunt
    // automated tampering with other members' event registrations.
    private fun rateLimitOrThrow(httpRequest: HttpServletRequest) {
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("event-register:$ip", maxRequests = 40, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.")
        }
    }
}
