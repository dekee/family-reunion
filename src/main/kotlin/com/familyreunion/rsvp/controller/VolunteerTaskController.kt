package com.familyreunion.rsvp.controller

import com.familyreunion.rsvp.dto.VolunteerSignupRequest
import com.familyreunion.rsvp.dto.VolunteerTaskRequest
import com.familyreunion.rsvp.dto.VolunteerTaskResponse
import com.familyreunion.rsvp.security.IpRateLimiter
import com.familyreunion.rsvp.service.VolunteerTaskService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/volunteer-tasks")
class VolunteerTaskController(
    private val volunteerTaskService: VolunteerTaskService,
    private val rateLimiter: IpRateLimiter
) {

    @GetMapping
    fun getAllTasks(): ResponseEntity<List<VolunteerTaskResponse>> {
        return ResponseEntity.ok(volunteerTaskService.getAllTasks())
    }

    @PostMapping
    fun createTask(@Valid @RequestBody request: VolunteerTaskRequest): ResponseEntity<VolunteerTaskResponse> {
        val response = volunteerTaskService.createTask(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @Valid @RequestBody request: VolunteerTaskRequest
    ): ResponseEntity<VolunteerTaskResponse> {
        return ResponseEntity.ok(volunteerTaskService.updateTask(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: Long): ResponseEntity<Void> {
        volunteerTaskService.deleteTask(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/signup")
    fun signUp(
        @PathVariable id: Long,
        @Valid @RequestBody request: VolunteerSignupRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<VolunteerTaskResponse> {
        rateLimitOrThrow(httpRequest)
        return ResponseEntity.ok(volunteerTaskService.signUp(id, request))
    }

    @DeleteMapping("/{id}/signup/{memberId}")
    fun withdraw(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        rateLimitOrThrow(httpRequest)
        volunteerTaskService.withdraw(id, memberId)
        return ResponseEntity.noContent().build()
    }

    // Public, unauthenticated signup endpoints: cap mutations per client IP to blunt
    // automated tampering with other members' volunteer signups.
    private fun rateLimitOrThrow(httpRequest: HttpServletRequest) {
        val ip = IpRateLimiter.clientIp(httpRequest)
        if (!rateLimiter.tryAcquire("volunteer-signup:$ip", maxRequests = 40, windowSeconds = 600)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.")
        }
    }
}
