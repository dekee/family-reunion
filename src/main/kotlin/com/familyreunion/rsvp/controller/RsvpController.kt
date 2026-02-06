package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.RsvpRequest
import com.familyreunion.rsvp.dto.RsvpResponse
import com.familyreunion.rsvp.dto.RsvpSummaryResponse
import com.familyreunion.rsvp.service.RsvpService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rsvp")
class RsvpController(private val rsvpService: RsvpService) {

    @PostMapping
    fun createRsvp(@Valid @RequestBody request: RsvpRequest): ResponseEntity<RsvpResponse> {
        val response = rsvpService.createRsvp(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getAllRsvps(): ResponseEntity<List<RsvpResponse>> {
        return ResponseEntity.ok(rsvpService.getAllRsvps())
    }

    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<RsvpSummaryResponse> {
        return ResponseEntity.ok(rsvpService.getSummary())
    }

    @GetMapping("/{id}")
    fun getRsvpById(@PathVariable id: Long): ResponseEntity<RsvpResponse> {
        return ResponseEntity.ok(rsvpService.getRsvpById(id))
    }

    @PutMapping("/{id}")
    fun updateRsvp(
        @PathVariable id: Long,
        @Valid @RequestBody request: RsvpRequest
    ): ResponseEntity<RsvpResponse> {
        return ResponseEntity.ok(rsvpService.updateRsvp(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteRsvp(@PathVariable id: Long): ResponseEntity<Void> {
        rsvpService.deleteRsvp(id)
        return ResponseEntity.noContent().build()
    }
}
