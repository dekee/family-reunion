package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.EventRegisterRequest
import com.familyreunion.rsvp.dto.EventRequest
import com.familyreunion.rsvp.dto.EventResponse
import com.familyreunion.rsvp.service.EventService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(private val eventService: EventService) {

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
        @Valid @RequestBody request: EventRegisterRequest
    ): ResponseEntity<EventResponse> {
        return ResponseEntity.ok(eventService.registerMembers(id, request))
    }

    @DeleteMapping("/{id}/register/{memberId}")
    fun unregisterMember(
        @PathVariable id: Long,
        @PathVariable memberId: Long
    ): ResponseEntity<Void> {
        eventService.unregisterMember(id, memberId)
        return ResponseEntity.noContent().build()
    }
}
