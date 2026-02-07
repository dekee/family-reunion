package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.MeetingRequest
import com.familyreunion.rsvp.dto.MeetingResponse
import com.familyreunion.rsvp.service.MeetingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/meetings")
class MeetingController(private val meetingService: MeetingService) {

    @PostMapping
    fun createMeeting(@Valid @RequestBody request: MeetingRequest): ResponseEntity<MeetingResponse> {
        val response = meetingService.createMeeting(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getAllMeetings(): ResponseEntity<List<MeetingResponse>> {
        return ResponseEntity.ok(meetingService.getAllMeetings())
    }

    @GetMapping("/{id}")
    fun getMeetingById(@PathVariable id: Long): ResponseEntity<MeetingResponse> {
        return ResponseEntity.ok(meetingService.getMeetingById(id))
    }

    @PutMapping("/{id}")
    fun updateMeeting(
        @PathVariable id: Long,
        @Valid @RequestBody request: MeetingRequest
    ): ResponseEntity<MeetingResponse> {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteMeeting(@PathVariable id: Long): ResponseEntity<Void> {
        meetingService.deleteMeeting(id)
        return ResponseEntity.noContent().build()
    }
}
